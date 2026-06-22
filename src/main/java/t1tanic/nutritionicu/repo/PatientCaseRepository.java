package t1tanic.nutritionicu.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.PatientCase;

public interface PatientCaseRepository extends JpaRepository<PatientCase, Long> {

    /** The case archived from a given patient, if any — for idempotent re-archiving. */
    Optional<PatientCase> findBySourcePatientId(Long sourcePatientId);

    /** Candidate cases within an age window — an index range scan that narrows before in-service ranking. */
    List<PatientCase> findByAgeYearsBetween(Integer low, Integer high);

    /** Whether any seeded synthetic case exists (those have no source patient) — guards demo seeding. */
    boolean existsBySourcePatientIdIsNull();
}
