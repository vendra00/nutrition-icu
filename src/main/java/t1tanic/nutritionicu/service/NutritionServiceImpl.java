package t1tanic.nutritionicu.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.dto.NutricScore;
import t1tanic.nutritionicu.dto.NutritionMetrics;
import t1tanic.nutritionicu.model.NutritionRiskAssessment;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.WeightMeasurement;
import t1tanic.nutritionicu.model.enums.AdmissionDelayBand;
import t1tanic.nutritionicu.model.enums.AgeBand;
import t1tanic.nutritionicu.model.enums.ApacheBand;
import t1tanic.nutritionicu.model.enums.ComorbidityBand;
import t1tanic.nutritionicu.model.enums.Il6Band;
import t1tanic.nutritionicu.model.enums.NutricBand;
import t1tanic.nutritionicu.model.enums.Sex;
import t1tanic.nutritionicu.model.enums.SofaBand;
import t1tanic.nutritionicu.repo.NutritionRiskAssessmentRepository;
import t1tanic.nutritionicu.repo.PatientRepository;
import t1tanic.nutritionicu.repo.WeightMeasurementRepository;

/**
 * Nutrition-protocol anthropometry: stores the doctor's inputs and derives BMI,
 * ideal/adjusted body weight (Devine) and recent weight-loss %. Weight is tracked
 * over time as dated {@link WeightMeasurement}s; the patient's current weight always
 * mirrors the most recent entry.
 */
@Service
public class NutritionServiceImpl implements NutritionService {

    private final PatientRepository patientRepository;
    private final WeightMeasurementRepository weightRepository;
    private final NutritionRiskAssessmentRepository riskRepository;

    public NutritionServiceImpl(PatientRepository patientRepository,
                                WeightMeasurementRepository weightRepository,
                                NutritionRiskAssessmentRepository riskRepository) {
        this.patientRepository = patientRepository;
        this.weightRepository = weightRepository;
        this.riskRepository = riskRepository;
    }

    @Override
    public NutritionMetrics metricsFor(Patient patient) {
        Double ibw = idealBodyWeight(patient.getHeightCm(), patient.getSex());
        return new NutritionMetrics(
                round(bmi(patient.getCurrentWeightKg(), patient.getHeightCm())),
                round(ibw),
                round(adjustedBodyWeight(patient.getCurrentWeightKg(), ibw)),
                round(weightLossPercent(patient.getUsualWeightKg(), patient.getCurrentWeightKg())));
    }

    @Override
    @Transactional
    public Patient updateAnthropometry(Long patientId, Double heightCm, Double usualWeightKg) {
        Patient patient = patient(patientId);
        patient.setHeightCm(heightCm);
        patient.setUsualWeightKg(usualWeightKg);
        return patientRepository.save(patient);
    }

    @Override
    @Transactional
    public WeightMeasurement recordWeight(Long patientId, LocalDate date, Double weightKg) {
        Patient patient = patient(patientId);
        WeightMeasurement measurement = weightRepository.findByPatientIdAndMeasuredOn(patientId, date)
                .orElseGet(() -> new WeightMeasurement(patient, date, weightKg));
        measurement.setWeightKg(weightKg);
        weightRepository.save(measurement);
        syncCurrentWeight(patient);
        return measurement;
    }

    @Override
    @Transactional
    public void deleteWeight(Long weightMeasurementId) {
        weightRepository.findById(weightMeasurementId).ifPresent(measurement -> {
            Patient patient = measurement.getPatient();
            weightRepository.delete(measurement);
            weightRepository.flush();
            syncCurrentWeight(patient);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeightMeasurement> weightHistory(Long patientId) {
        return weightRepository.findByPatientIdOrderByMeasuredOnAsc(patientId);
    }

    @Override
    public NutricScore computeNutric(AgeBand age, ApacheBand apache, SofaBand sofa,
                                     ComorbidityBand comorbidity, AdmissionDelayBand admissionDelay, Il6Band il6) {
        int score = points(age) + points(apache) + points(sofa)
                + points(comorbidity) + points(admissionDelay);
        boolean includesIl6 = il6 != null;
        if (includesIl6) {
            score += il6.points();
        }
        int max = includesIl6 ? 10 : 9;
        boolean highRisk = includesIl6 ? score >= 6 : score >= 5;
        return new NutricScore(score, max, highRisk, includesIl6);
    }

    @Override
    @Transactional
    public NutritionRiskAssessment recordRiskAssessment(Long patientId, LocalDate date, ApacheBand apache,
                                                        SofaBand sofa, ComorbidityBand comorbidity,
                                                        AdmissionDelayBand admissionDelay, Il6Band il6) {
        Patient patient = patient(patientId);
        int age = patient.getBirthDate() != null
                ? Period.between(patient.getBirthDate(), date).getYears() : 0;
        AgeBand ageBand = AgeBand.fromAge(age);
        NutricScore nutric = computeNutric(ageBand, apache, sofa, comorbidity, admissionDelay, il6);

        NutritionRiskAssessment assessment = new NutritionRiskAssessment(patient, date);
        assessment.setAgeBand(ageBand);
        assessment.setApacheBand(apache);
        assessment.setSofaBand(sofa);
        assessment.setComorbidityBand(comorbidity);
        assessment.setAdmissionDelayBand(admissionDelay);
        assessment.setIl6Band(il6);
        assessment.setNutricScore(nutric.score());
        assessment.setNutricMax(nutric.maxScore());
        assessment.setHighRisk(nutric.highRisk());
        return riskRepository.save(assessment);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NutritionRiskAssessment> latestRiskAssessment(Long patientId) {
        return riskRepository.findTopByPatientIdOrderByAssessedOnDescIdDesc(patientId);
    }

    private static int points(NutricBand band) {
        return band == null ? 0 : band.points();
    }

    /** Current weight always reflects the most recent dated measurement (or null if none). */
    private void syncCurrentWeight(Patient patient) {
        Double latest = weightRepository.findByPatientIdOrderByMeasuredOnAsc(patient.getId()).stream()
                .reduce((first, second) -> second)
                .map(WeightMeasurement::getWeightKg)
                .orElse(null);
        patient.setCurrentWeightKg(latest);
        patientRepository.save(patient);
    }

    private Patient patient(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("No patient with id " + patientId));
    }

    /** BMI = weight(kg) / height(m)². */
    private static Double bmi(Double weightKg, Double heightCm) {
        if (weightKg == null || heightCm == null || heightCm <= 0) {
            return null;
        }
        double m = heightCm / 100.0;
        return weightKg / (m * m);
    }

    /** Devine ideal body weight: base + 2.3 kg per inch over 5 ft (base 50 male / 45.5 female). */
    private static Double idealBodyWeight(Double heightCm, Sex sex) {
        if (heightCm == null || sex == null || sex == Sex.UNKNOWN) {
            return null;
        }
        double inches = heightCm / 2.54;
        double base = sex == Sex.MALE ? 50.0 : 45.5;
        return base + 2.3 * (inches - 60.0);
    }

    /** Adjusted body weight for obesity: IBW + 0.4·(actual − IBW); just the actual weight if not over IBW. */
    private static Double adjustedBodyWeight(Double currentKg, Double idealKg) {
        if (currentKg == null || idealKg == null) {
            return null;
        }
        return currentKg <= idealKg ? currentKg : idealKg + 0.4 * (currentKg - idealKg);
    }

    /** Recent weight loss as a percentage of usual weight (positive = loss). */
    private static Double weightLossPercent(Double usualKg, Double currentKg) {
        if (usualKg == null || currentKg == null || usualKg <= 0) {
            return null;
        }
        return (usualKg - currentKg) / usualKg * 100.0;
    }

    private static Double round(Double value) {
        return value == null ? null : Math.round(value * 10.0) / 10.0;
    }
}
