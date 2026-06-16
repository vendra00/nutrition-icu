package t1tanic.nutritionicu.service;

import java.time.LocalDate;
import java.util.List;
import t1tanic.nutritionicu.dto.NutritionMetrics;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.WeightMeasurement;

/** Nutrition-protocol calculations and the clinician-entered anthropometry data. */
public interface NutritionService {

    /** Computes BMI, ideal/adjusted body weight and weight-loss % from a patient's anthropometry. */
    NutritionMetrics metricsFor(Patient patient);

    /** Updates the static anthropometry (height, usual weight) a doctor records at screening. */
    Patient updateAnthropometry(Long patientId, Double heightCm, Double usualWeightKg);

    /** Records (or updates) the patient's weight for a date; keeps current weight on the latest. */
    WeightMeasurement recordWeight(Long patientId, LocalDate date, Double weightKg);

    /** Removes a weight entry and re-syncs the patient's current weight. */
    void deleteWeight(Long weightMeasurementId);

    /** A patient's weight history, oldest first. */
    List<WeightMeasurement> weightHistory(Long patientId);
}
