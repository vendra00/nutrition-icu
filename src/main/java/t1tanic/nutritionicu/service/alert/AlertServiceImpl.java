package t1tanic.nutritionicu.service.alert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.dto.AlertFilter;
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
 * Raises alerts for monitored patients, but only for the lab markers that matter to the ICU nutrition
 * team — glycemic control, visceral proteins, inflammation/metabolic-phase markers, lipid tolerance, and
 * the refeeding-syndrome electrolytes. Other abnormal results (cardiac, renal function, liver enzymes,
 * haematology, etc.) are intentionally ignored so the nutrition segment isn't flooded with noise.
 */
@Slf4j
@Service
public class AlertServiceImpl implements AlertService {

    /**
     * Canonical analyte codes the nutrition team alerts on. Includes refeeding-syndrome electrolytes
     * (phosphate, potassium, magnesium) — the most important nutrition alerts when feeding starts.
     */
    private static final Set<String> NUTRITION_ANALYTES = Set.of(
            "GLUCOSE", "UREA", "LACTATE",
            "PREALBUMIN", "ALBUMIN", "TOTAL_PROTEIN",
            "CRP", "PROCALCITONIN",
            "TRIGLYCERIDES",
            "PHOSPHATE", "POTASSIUM", "MAGNESIUM", "CALCIUM", "CALCIUM_IONIZED", "SODIUM");

    /** Low phosphate / potassium / magnesium signals refeeding syndrome — always escalated to CRITICAL. */
    private static final Set<String> REFEEDING_ELECTROLYTES = Set.of("PHOSPHATE", "POTASSIUM", "MAGNESIUM");

    /** Nutrition alerts are routed to the nutrition team within the ICU. */
    private static final Set<Sector> NUTRITION_SECTORS = Set.of(Sector.ICU, Sector.NUTRITION);

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
        if (report.getId() != null && alertRepository.existsByReportId(report.getId())) {
            return Optional.empty(); // already evaluated this report — stays idempotent across restarts
        }

        List<LabResult> abnormal = report.getSections().stream()
                .flatMap(section -> section.getResults().stream())
                .filter(result -> result.getFlag() != null && result.getFlag() != ResultFlag.NORMAL)
                .filter(result -> result.getAnalyteCode() != null
                        && NUTRITION_ANALYTES.contains(result.getAnalyteCode()))
                .toList();
        if (abnormal.isEmpty()) {
            return Optional.empty();
        }

        Set<Sector> sectors = new HashSet<>(NUTRITION_SECTORS);
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
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertSummary> search(AlertFilter filter, int offset, int limit) {
        int size = Math.max(limit, 1);
        Pageable pageable = PageRequest.of(offset / size, size);
        return alertRepository.search(filter.severity(), filter.status(), filter.patientMrn(), filter.text(), pageable)
                .stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long count(AlertFilter filter) {
        return alertRepository.countSearch(filter.severity(), filter.status(), filter.patientMrn(), filter.text());
    }

    private AlertSummary toSummary(Alert alert) {
        return new AlertSummary(
                alert.getId(),
                alert.getSeverity().name(),
                alert.getStatus().name(),
                alert.getPatient().getMedicalRecordNumber(),
                alert.getTargetSectors().stream().map(Enum::name).sorted().collect(Collectors.joining(", ")),
                alert.getMessage(),
                alert.getCreatedAt());
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

    private AlertSeverity severityOf(List<LabResult> abnormal) {
        if (hasRefeedingRisk(abnormal)) {
            return AlertSeverity.CRITICAL;
        }
        boolean critical = abnormal.stream().anyMatch(result ->
                result.getFlag() == ResultFlag.VERY_HIGH || result.getFlag() == ResultFlag.VERY_LOW);
        return critical ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
    }

    /** A low refeeding electrolyte (phosphate/potassium/magnesium) is the classic refeeding-syndrome signal. */
    private static boolean hasRefeedingRisk(List<LabResult> abnormal) {
        return abnormal.stream().anyMatch(result ->
                REFEEDING_ELECTROLYTES.contains(result.getAnalyteCode()) && isLow(result.getFlag()));
    }

    private static boolean isLow(ResultFlag flag) {
        return flag == ResultFlag.LOW || flag == ResultFlag.VERY_LOW;
    }

    private String buildMessage(List<LabResult> abnormal) {
        String detail = abnormal.stream()
                .map(result -> "%s %s%s (%s)".formatted(
                        analyteCatalog.displayName(result.getAnalyteName()),
                        result.getValueRaw(),
                        result.getUnit() != null ? " " + result.getUnit().getSymbol() : "",
                        result.getFlag()))
                .collect(Collectors.joining("; "));
        String prefix = hasRefeedingRisk(abnormal) ? "Possible refeeding syndrome — " : "";
        return prefix + "%d abnormal result(s): %s".formatted(abnormal.size(), detail);
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
