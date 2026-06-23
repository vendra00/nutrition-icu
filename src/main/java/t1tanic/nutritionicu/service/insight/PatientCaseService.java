package t1tanic.nutritionicu.service.insight;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.model.BodyCompositionMeasurement;
import t1tanic.nutritionicu.model.LabResult;
import t1tanic.nutritionicu.model.NutritionRiskAssessment;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.PatientCase;
import t1tanic.nutritionicu.model.WeightMeasurement;
import t1tanic.nutritionicu.model.enums.AdmissionDiagnosis;
import t1tanic.nutritionicu.model.enums.Sex;
import t1tanic.nutritionicu.repo.PatientCaseRepository;
import t1tanic.nutritionicu.service.lab.LabResultService;
import t1tanic.nutritionicu.service.nutrition.NutritionService;
import t1tanic.nutritionicu.service.patient.PatientService;

/**
 * Manages the anonymized {@link PatientCase} archive and builds the comparison cohort from it. Archiving
 * snapshots a patient's de-identified course; comparison ranks archived cases against an index patient
 * by age/BMI/NUTRIC/SOFA, narrowing first by the indexed age window for performance.
 */
@Service
public class PatientCaseService {

    private static final double AGE_SCALE = 15.0;
    private static final double BMI_SCALE = 6.0;
    private static final double NUTRIC_SCALE = 3.0;
    private static final double SOFA_SCALE = 2.0;
    /** Squared-distance penalty added when two cases have a different admission diagnosis (prefers same). */
    private static final double DIFFERENT_DIAGNOSIS_PENALTY = 3.0;

    /** Age half-window (years) for the indexed candidate pre-filter. */
    private static final int AGE_WINDOW = 25;

    private final PatientCaseRepository caseRepository;
    private final PatientService patientService;
    private final NutritionService nutritionService;
    private final LabResultService labService;

    public PatientCaseService(PatientCaseRepository caseRepository, PatientService patientService,
                              NutritionService nutritionService, LabResultService labService) {
        this.caseRepository = caseRepository;
        this.patientService = patientService;
        this.nutritionService = nutritionService;
        this.labService = labService;
    }

    /** The de-identified comparison context: the index features plus the nearest archived cases. */
    public record Cohort(int size, String text) {
    }

    /** The index patient's features plus the nearest archived cases as entities, for charting. */
    public record CohortComparison(Integer age, Sex sex, Double bmi, Integer nutric, Integer nutricMax,
                                   Boolean highRisk, List<PatientCase> peers) {
        public boolean hasPeers() {
            return !peers.isEmpty();
        }
    }

    public long archiveSize() {
        return caseRepository.count();
    }

    /**
     * Snapshots a patient into the anonymized case archive (idempotent: re-archiving updates the same
     * case). Returns the stored case.
     */
    @Transactional
    public PatientCase archive(Long patientId) {
        Patient patient = patientService.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown patient: " + patientId));
        Features f = features(patient);

        PatientCase c = caseRepository.findBySourcePatientId(patientId).orElseGet(PatientCase::new);
        c.setSourcePatientId(patientId);
        c.setAdmissionDiagnosis(patient.getAdmissionDiagnosis());
        c.setAgeYears(f.age() == null ? null : (int) Math.round(f.age()));
        c.setSex(patient.getSex());
        c.setBmi(f.bmi());
        c.setNutricScore(f.nutric());
        c.setNutricMax(f.nutricMax());
        c.setHighRisk(f.highRisk());
        c.setSofaOrdinal(f.sofaOrdinal());
        c.setSofaBand(f.sofaLabel());
        c.setLengthOfStayDays(stayDays(patient));
        c.setDischarged(patient.getDischargeDate() != null);
        c.setCourseText(courseText(patient, f));

        PatientCase saved = caseRepository.save(c);
        if (saved.getCaseCode() == null) {
            saved.setCaseCode(String.format("CASE-%04d", saved.getId()));
            saved = caseRepository.save(saved);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public Cohort cohortContext(Long indexPatientId, int maxPeers) {
        Patient index = patientService.findById(indexPatientId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown patient: " + indexPatientId));
        Features idx = features(index);
        if (!idx.comparable()) {
            return new Cohort(0, "");
        }
        List<PatientCase> nearest = nearestCases(indexPatientId, idx, maxPeers);
        if (nearest.isEmpty()) {
            return new Cohort(0, "");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("INDEX PATIENT (the one being compared):\n");
        sb.append("- ").append(featuresLine(index, idx)).append('\n');
        sb.append("\nSIMILAR ARCHIVED CASES (de-identified, most similar first):\n");
        int i = 1;
        for (PatientCase peer : nearest) {
            sb.append(i++).append(". [").append(peer.getCaseCode()).append("] ")
                    .append(peer.getCourseText()).append('\n');
        }
        return new Cohort(nearest.size(), sb.toString().strip());
    }

    /** The index patient's features plus the nearest archived cases as entities, for the comparison charts. */
    @Transactional(readOnly = true)
    public CohortComparison compareCohort(Long indexPatientId, int maxPeers) {
        Patient index = patientService.findById(indexPatientId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown patient: " + indexPatientId));
        Features idx = features(index);
        if (!idx.comparable()) {
            return new CohortComparison(null, index.getSex(), null, null, null, null, List.of());
        }
        Integer ageYears = idx.age() == null ? null : (int) Math.round(idx.age());
        return new CohortComparison(ageYears, index.getSex(), idx.bmi(), idx.nutric(), idx.nutricMax(),
                idx.highRisk(), nearestCases(indexPatientId, idx, maxPeers));
    }

    /** Ranks the archive against the index features and returns the nearest cases (indexed age pre-filter). */
    private List<PatientCase> nearestCases(Long indexPatientId, Features idx, int maxPeers) {
        int age = (int) Math.round(idx.age());
        List<PatientCase> candidates = caseRepository.findByAgeYearsBetween(age - AGE_WINDOW, age + AGE_WINDOW);
        if (candidates.size() < maxPeers) {
            candidates = caseRepository.findAll(); // archive still small - rank them all
        }
        List<Scored> ranked = new ArrayList<>();
        for (PatientCase c : candidates) {
            if (indexPatientId.equals(c.getSourcePatientId()) || c.getBmi() == null || c.getAgeYears() == null) {
                continue; // skip the patient's own case and incomparable ones
            }
            ranked.add(new Scored(c, distance(idx, caseFeatures(c))));
        }
        ranked.sort(Comparator.comparingDouble(Scored::distance));
        return ranked.subList(0, Math.min(maxPeers, ranked.size())).stream().map(Scored::patientCase).toList();
    }

    // --- feature extraction & distance ---

    private record Features(Double age, Double bmi, Integer nutric, Integer nutricMax, Integer sofaOrdinal,
                            String sofaLabel, Boolean highRisk, AdmissionDiagnosis diagnosis) {
        boolean comparable() {
            return age != null && bmi != null;
        }
    }

    private record Scored(PatientCase patientCase, double distance) {
    }

    private Features features(Patient patient) {
        Integer ageInt = patient.ageOn(LocalDate.now());
        Double age = ageInt == null ? null : ageInt.doubleValue();
        Double bmi = nutritionService.metricsFor(patient).bmi();
        Optional<NutritionRiskAssessment> risk = nutritionService.latestRiskAssessment(patient.getId());
        Integer nutric = risk.map(NutritionRiskAssessment::getNutricScore).orElse(null);
        Integer nutricMax = risk.map(NutritionRiskAssessment::getNutricMax).orElse(null);
        Integer sofaOrdinal = risk.map(r -> r.getSofaBand() == null ? null : r.getSofaBand().ordinal()).orElse(null);
        String sofaLabel = risk.map(r -> r.getSofaBand() == null ? null : r.getSofaBand().name()).orElse(null);
        Boolean highRisk = risk.map(NutritionRiskAssessment::isHighRisk).orElse(null);
        return new Features(age, bmi, nutric, nutricMax, sofaOrdinal, sofaLabel, highRisk,
                patient.getAdmissionDiagnosis());
    }

    private static Features caseFeatures(PatientCase c) {
        Double age = c.getAgeYears() == null ? null : c.getAgeYears().doubleValue();
        return new Features(age, c.getBmi(), c.getNutricScore(), c.getNutricMax(), c.getSofaOrdinal(),
                c.getSofaBand(), c.getHighRisk(), c.getAdmissionDiagnosis());
    }

    private static double distance(Features a, Features b) {
        double d = sq((a.age() - b.age()) / AGE_SCALE) + sq((a.bmi() - b.bmi()) / BMI_SCALE);
        if (a.nutric() != null && b.nutric() != null) {
            d += sq((a.nutric() - b.nutric()) / NUTRIC_SCALE);
        }
        if (a.sofaOrdinal() != null && b.sofaOrdinal() != null) {
            d += sq((a.sofaOrdinal() - b.sofaOrdinal()) / SOFA_SCALE);
        }
        if (a.diagnosis() != null && b.diagnosis() != null && a.diagnosis() != b.diagnosis()) {
            d += DIFFERENT_DIAGNOSIS_PENALTY; // prefer same-condition cases
        }
        return Math.sqrt(d);
    }

    private static double sq(double v) {
        return v * v;
    }

    // --- de-identified narrative ---

    private String featuresLine(Patient p, Features f) {
        StringBuilder sb = new StringBuilder();
        if (f.diagnosis() != null) {
            sb.append("Diagnosis: ").append(f.diagnosis().label()).append("; ");
        }
        sb.append("Age ").append(intOf(f.age())).append(", ").append(sexOf(p.getSex()));
        sb.append("; BMI ").append(num(f.bmi()));
        if (f.nutric() != null) {
            sb.append("; NUTRIC ").append(f.nutric());
            if (f.nutricMax() != null) {
                sb.append('/').append(f.nutricMax());
            }
            if (Boolean.TRUE.equals(f.highRisk())) {
                sb.append(" (high risk)");
            } else if (Boolean.FALSE.equals(f.highRisk())) {
                sb.append(" (low risk)");
            }
        }
        if (f.sofaLabel() != null) {
            sb.append("; SOFA ").append(f.sofaLabel());
        }
        return sb.toString();
    }

    private String courseText(Patient p, Features f) {
        StringBuilder sb = new StringBuilder(featuresLine(p, f));
        sb.append("; ").append(stayText(p));
        List<String> traj = new ArrayList<>();
        addTrajectory(traj, p.getId(), "ALBUMIN", "Albumin");
        addTrajectory(traj, p.getId(), "PREALBUMIN", "Prealbumin");
        addTrajectory(traj, p.getId(), "CRP", "CRP");
        addTrajectory(traj, p.getId(), "PHOSPHATE", "Phosphate");
        String weight = weightTrajectory(p.getId());
        if (weight != null) {
            traj.add(weight);
        }
        String bodyComp = bodyCompTrajectory(p.getId());
        if (bodyComp != null) {
            traj.add(bodyComp);
        }
        if (!traj.isEmpty()) {
            sb.append("; ").append(String.join("; ", traj));
        }
        return sb.toString();
    }

    /** Skeletal-muscle first->latest trajectory for the archived course, flagging muscle loss. */
    private String bodyCompTrajectory(Long patientId) {
        List<BodyCompositionMeasurement> history = nutritionService.bodyCompositionHistory(patientId);
        BodyCompositionMeasurement first = null;
        BodyCompositionMeasurement last = null;
        for (BodyCompositionMeasurement m : history) {
            if (m.getSkeletalMusclePercent() != null) {
                if (first == null) {
                    first = m;
                }
                last = m;
            }
        }
        if (first == null) {
            return null;
        }
        if (first == last) {
            return "skeletal muscle " + num(first.getSkeletalMusclePercent()) + " %";
        }
        String trend = "skeletal muscle " + num(first.getSkeletalMusclePercent()) + "->"
                + num(last.getSkeletalMusclePercent()) + " %";
        return last.getSkeletalMusclePercent() < first.getSkeletalMusclePercent() ? trend + " (muscle loss)" : trend;
    }

    private void addTrajectory(List<String> out, Long patientId, String code, String label) {
        List<LabResult> numeric = labService.seriesByCode(patientId, code).stream()
                .filter(r -> r.getValueNumeric() != null)
                .toList();
        if (numeric.isEmpty()) {
            return;
        }
        double first = numeric.getFirst().getValueNumeric().doubleValue();
        double last = numeric.getLast().getValueNumeric().doubleValue();
        String unit = numeric.getLast().getUnitRaw();
        out.add(label + " " + num(first) + "->" + num(last) + (unit == null || unit.isBlank() ? "" : " " + unit));
    }

    private String weightTrajectory(Long patientId) {
        List<WeightMeasurement> weights = nutritionService.weightHistory(patientId);
        if (weights.size() < 2 || weights.getFirst().getWeightKg() == null || weights.getLast().getWeightKg() == null) {
            return null;
        }
        return "weight " + num(weights.getFirst().getWeightKg()) + "->" + num(weights.getLast().getWeightKg()) + " kg";
    }

    private static Integer stayDays(Patient p) {
        if (p.getAdmissionDate() == null || p.getDischargeDate() == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(p.getAdmissionDate(), p.getDischargeDate());
    }

    private static String stayText(Patient p) {
        Integer days = stayDays(p);
        if (days != null) {
            return "stay " + days + " days (discharged)";
        }
        return p.getAdmissionDate() != null ? "still admitted" : "stay unknown";
    }

    private static String sexOf(t1tanic.nutritionicu.model.enums.Sex sex) {
        return sex == null ? "sex n/a" : sex.name().toLowerCase(Locale.ROOT);
    }

    private static String intOf(Double value) {
        return value == null ? "n/a" : String.valueOf(Math.round(value));
    }

    private static String num(Double value) {
        return value == null ? "n/a" : String.format(Locale.US, "%.1f", value);
    }
}
