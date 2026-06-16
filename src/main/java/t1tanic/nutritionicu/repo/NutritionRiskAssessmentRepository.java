package t1tanic.nutritionicu.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.NutritionRiskAssessment;

public interface NutritionRiskAssessmentRepository extends JpaRepository<NutritionRiskAssessment, Long> {

    /** The most recent assessment for a patient. */
    Optional<NutritionRiskAssessment> findTopByPatientIdOrderByAssessedOnDescIdDesc(Long patientId);

    /** A patient's assessments over time, oldest first. */
    List<NutritionRiskAssessment> findByPatientIdOrderByAssessedOnAsc(Long patientId);
}
