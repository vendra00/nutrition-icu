package t1tanic.nutritionicu.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.EnergyAssessment;
import t1tanic.nutritionicu.model.enums.EnergyMethod;

public interface EnergyAssessmentRepository extends JpaRepository<EnergyAssessment, Long> {

    /** A patient's energy assessments (both methods), oldest first. */
    List<EnergyAssessment> findByPatientIdOrderByAssessedOnAsc(Long patientId);

    /** A patient's assessments for one method, oldest first — the per-method trend series. */
    List<EnergyAssessment> findByPatientIdAndMethodOrderByAssessedOnAsc(Long patientId, EnergyMethod method);

    /** The entry for a specific date and method, if any (used to upsert on edit). */
    Optional<EnergyAssessment> findByPatientIdAndAssessedOnAndMethod(
            Long patientId, LocalDate assessedOn, EnergyMethod method);

    /** The most recent assessment for one method, if any. */
    Optional<EnergyAssessment> findTopByPatientIdAndMethodOrderByAssessedOnDesc(
            Long patientId, EnergyMethod method);
}
