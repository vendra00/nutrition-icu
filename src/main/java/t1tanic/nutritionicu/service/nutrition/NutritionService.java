package t1tanic.nutritionicu.service.nutrition;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import t1tanic.nutritionicu.dto.NutricScore;
import t1tanic.nutritionicu.dto.NutritionMetrics;
import t1tanic.nutritionicu.model.BodyCompositionMeasurement;
import t1tanic.nutritionicu.model.NutritionRiskAssessment;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.TemperatureMeasurement;
import t1tanic.nutritionicu.model.WeightMeasurement;
import t1tanic.nutritionicu.model.enums.AdmissionDelayBand;
import t1tanic.nutritionicu.model.enums.AgeBand;
import t1tanic.nutritionicu.model.enums.ApacheBand;
import t1tanic.nutritionicu.model.enums.ComorbidityBand;
import t1tanic.nutritionicu.model.enums.Il6Band;
import t1tanic.nutritionicu.model.enums.SofaBand;

/** Nutrition-protocol calculations and the clinician-entered anthropometry data. */
public interface NutritionService {

    /** Computes BMI, ideal/adjusted body weight and weight-loss % from a patient's anthropometry. */
    NutritionMetrics metricsFor(Patient patient);

    /** Updates the static anthropometry (height, usual weight) a doctor records at screening. */
    Patient updateAnthropometry(Long patientId, Double heightCm, Double usualWeightKg);

    /** Records (or updates) the patient's weight for a date; keeps current weight on the latest. */
    WeightMeasurement recordWeight(Long patientId, LocalDate date, Double weightKg);

    /** Removes a weight entry and re-syncs the patient's current weight. Admin only. */
    @PreAuthorize("hasRole('ADMIN')")
    void deleteWeight(Long weightMeasurementId);

    /** A patient's weight history, oldest first. */
    List<WeightMeasurement> weightHistory(Long patientId);

    /** The patient's most recent weight reading, if any (the source of the current weight). */
    Optional<WeightMeasurement> latestWeight(Long patientId);

    /** Records (or updates) the patient's body temperature for a date. Tracking only; no calculation. */
    TemperatureMeasurement recordTemperature(Long patientId, LocalDate date, Double temperatureCelsius);

    /** Removes a temperature entry. Admin only. */
    @PreAuthorize("hasRole('ADMIN')")
    void deleteTemperature(Long temperatureMeasurementId);

    /** A patient's temperature history, oldest first. */
    List<TemperatureMeasurement> temperatureHistory(Long patientId);

    /** The patient's most recent temperature reading, if any. */
    Optional<TemperatureMeasurement> latestTemperature(Long patientId);

    /** Records (or updates) the patient's body-composition reading for a date. Any field may be null. */
    BodyCompositionMeasurement recordBodyComposition(Long patientId, LocalDate date, Double bodyFatPercent,
                                                     Double skeletalMusclePercent, Double boneDensity,
                                                     Double phaseAngle);

    /** Removes a body-composition entry. Admin only. */
    @PreAuthorize("hasRole('ADMIN')")
    void deleteBodyComposition(Long bodyCompositionMeasurementId);

    /** A patient's body-composition history, oldest first. */
    List<BodyCompositionMeasurement> bodyCompositionHistory(Long patientId);

    /** The patient's most recent body-composition reading, if any. */
    Optional<BodyCompositionMeasurement> latestBodyComposition(Long patientId);

    /** Computes the NUTRIC score from the banded severity inputs (IL-6 band optional). */
    NutricScore computeNutric(AgeBand age, ApacheBand apache, SofaBand sofa,
                              ComorbidityBand comorbidity, AdmissionDelayBand admissionDelay, Il6Band il6);

    /** Stores a dated risk assessment, computing and saving its NUTRIC score. Age band is derived. */
    NutritionRiskAssessment recordRiskAssessment(Long patientId, LocalDate date, ApacheBand apache,
                                                 SofaBand sofa, ComorbidityBand comorbidity,
                                                 AdmissionDelayBand admissionDelay, Il6Band il6);

    /** The most recent risk assessment for a patient, if any. */
    Optional<NutritionRiskAssessment> latestRiskAssessment(Long patientId);
}
