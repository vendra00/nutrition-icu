package t1tanic.nutritionicu.service.nutrition;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import t1tanic.nutritionicu.dto.EnergyExpenditureResult;
import t1tanic.nutritionicu.model.EnergyAssessment;
import t1tanic.nutritionicu.model.enums.EnergyMethod;

/**
 * Stores dated energy-expenditure assessments from both methods (Harris-Benedict and indirect
 * calorimetry) in one comparable structure, so a patient's energy targets can be trended and
 * measured-vs-predicted compared for research.
 */
public interface EnergyAssessmentService {

    /** Saves (or updates) a Harris-Benedict assessment for a date. */
    EnergyAssessment recordHarrisBenedict(Long patientId, LocalDate date,
                                          EnergyExpenditureResult result, double actualWeightKg);

    /** Saves (or updates) an indirect-calorimetry study for a date; derives weight/BMI/kcal-per-kg. */
    EnergyAssessment recordCalorimetry(Long patientId, LocalDate date, int measuredKcalPerDay, Double rq);

    /** Removes an assessment. */
    void delete(Long assessmentId);

    /** A patient's assessments across both methods, oldest first. */
    List<EnergyAssessment> history(Long patientId);

    /** A patient's assessments for one method, oldest first. */
    List<EnergyAssessment> history(Long patientId, EnergyMethod method);

    /** The patient's most recent assessment for a method, if any. */
    Optional<EnergyAssessment> latest(Long patientId, EnergyMethod method);
}
