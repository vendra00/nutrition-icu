package t1tanic.nutritionicu.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.AiInsight;

public interface AiInsightRepository extends JpaRepository<AiInsight, Long> {

    /** Most recent stored insight for a patient whose input matches exactly — the cache hit. */
    Optional<AiInsight> findFirstByPatientIdAndInputHashOrderByIdDesc(Long patientId, String inputHash);

    /** The original insight for the same clinical input (any language) — the source to translate from. */
    Optional<AiInsight> findFirstByPatientIdAndContentHashOrderByIdAsc(Long patientId, String contentHash);

    /** A patient's saved insights, newest first. */
    List<AiInsight> findByPatientIdOrderByCreatedAtDescIdDesc(Long patientId);
}
