package t1tanic.nutritionicu.service.alert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.dto.AlertSummary;
import t1tanic.nutritionicu.model.Alert;
import t1tanic.nutritionicu.model.Doctor;
import t1tanic.nutritionicu.model.LabReport;
import t1tanic.nutritionicu.model.LabResult;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.enums.AlertSeverity;
import t1tanic.nutritionicu.model.enums.ResultFlag;
import t1tanic.nutritionicu.model.enums.Sector;
import t1tanic.nutritionicu.repo.AlertRepository;
import t1tanic.nutritionicu.repo.DoctorRepository;
import t1tanic.nutritionicu.repo.LabReportRepository;
import t1tanic.nutritionicu.repo.PatientRepository;
import t1tanic.nutritionicu.service.lab.AnalyteCatalog;

/**
 * Raises alerts for monitored patients whose reports contain out-of-range results,
 * routing them to the relevant clinical sectors and notifying their doctors.
 */
@Slf4j
@Service
public class AlertServiceImpl implements AlertService {

    /** ICU always sees alerts for monitored patients; specialties are added per analyte. */
    private static final Sector BASELINE_SECTOR = Sector.ICU;

    /** Which specialty cares about a given canonical analyte (beyond the ICU baseline). */
    private static final Map<String, Sector> ANALYTE_SECTORS = Map.of(
            "GLUCOSE", Sector.ENDOCRINOLOGY,
            "CREATININE", Sector.NEPHROLOGY,
            "UREA", Sector.NEPHROLOGY,
            "AST", Sector.INTERNAL_MEDICINE,
            "ALT", Sector.INTERNAL_MEDICINE,
            "BILIRUBIN_TOTAL", Sector.INTERNAL_MEDICINE,
            "BILIRUBIN_DIRECT", Sector.INTERNAL_MEDICINE);

    private final AlertRepository alertRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final LabReportRepository reportRepository;
    private final AnalyteCatalog analyteCatalog;

    public AlertServiceImpl(AlertRepository alertRepository,
                            DoctorRepository doctorRepository,
                            PatientRepository patientRepository,
                            LabReportRepository reportRepository,
                            AnalyteCatalog analyteCatalog) {
        this.alertRepository = alertRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.reportRepository = reportRepository;
        this.analyteCatalog = analyteCatalog;
    }

    @Override
    public Optional<Alert> evaluate(LabReport report) {
        Patient patient = report.getPatient();
        if (patient == null || !patient.isMonitored()) {
            return Optional.empty();
        }

        List<LabResult> abnormal = report.getSections().stream()
                .flatMap(section -> section.getResults().stream())
                .filter(result -> result.getFlag() != null && result.getFlag() != ResultFlag.NORMAL)
                .toList();
        if (abnormal.isEmpty()) {
            return Optional.empty();
        }

        Set<Sector> sectors = targetSectors(abnormal);
        Alert alert = new Alert();
        alert.setPatient(patient);
        alert.setReport(report);
        alert.setSeverity(severityOf(abnormal));
        alert.setTargetSectors(sectors);
        alert.setAbnormalResults(new ArrayList<>(abnormal));
        alert.setMessage(buildMessage(abnormal));
        alertRepository.save(alert);

        notifyDoctors(alert, patient, sectors);
        return Optional.of(alert);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertSummary> recentAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(alert -> new AlertSummary(
                        alert.getId(),
                        alert.getSeverity().name(),
                        alert.getStatus().name(),
                        alert.getPatient().getMedicalRecordNumber(),
                        alert.getTargetSectors().stream().map(Enum::name).sorted()
                                .collect(Collectors.joining(", ")),
                        alert.getMessage(),
                        alert.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public int evaluateForMonitoredPatients() {
        int created = 0;
        for (Patient patient : patientRepository.findByMonitoredTrue()) {
            for (LabReport report : reportRepository.findByPatientId(patient.getId())) {
                if (evaluate(report).isPresent()) {
                    created++;
                }
            }
        }
        return created;
    }

    /** ICU plus any specialty implicated by the abnormal analytes (one to many). */
    private Set<Sector> targetSectors(List<LabResult> abnormal) {
        Set<Sector> sectors = new HashSet<>();
        sectors.add(BASELINE_SECTOR);
        for (LabResult result : abnormal) {
            String code = result.getAnalyteCode();
            if (code == null) {
                continue; // not yet canonicalized; ICU baseline still covers it
            }
            Sector sector = ANALYTE_SECTORS.get(code);
            if (sector != null) {
                sectors.add(sector);
            }
        }
        return sectors;
    }

    private AlertSeverity severityOf(List<LabResult> abnormal) {
        boolean critical = abnormal.stream().anyMatch(result ->
                result.getFlag() == ResultFlag.VERY_HIGH || result.getFlag() == ResultFlag.VERY_LOW);
        return critical ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
    }

    private String buildMessage(List<LabResult> abnormal) {
        String detail = abnormal.stream()
                .map(result -> "%s %s%s (%s)".formatted(
                        analyteCatalog.displayName(result.getAnalyteName()),
                        result.getValueRaw(),
                        result.getUnit() != null ? " " + result.getUnit().getSymbol() : "",
                        result.getFlag()))
                .collect(Collectors.joining("; "));
        return "%d abnormal result(s): %s".formatted(abnormal.size(), detail);
    }

    private void notifyDoctors(Alert alert, Patient patient, Set<Sector> sectors) {
        List<Doctor> recipients = doctorRepository.findBySectorIn(sectors);
        log.warn("ALERT [{}] patient {} -> sectors {} ({} doctor(s)): {}",
                alert.getSeverity(), patient.getMedicalRecordNumber(), sectors, recipients.size(),
                alert.getMessage());
        for (Doctor doctor : recipients) {
            // TODO: real delivery (email/push). For now, log per recipient.
            log.warn("  -> notifying {} ({})", doctor.getName(), doctor.getSector());
        }
    }
}
