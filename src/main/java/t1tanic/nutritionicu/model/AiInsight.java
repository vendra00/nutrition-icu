package t1tanic.nutritionicu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import t1tanic.nutritionicu.model.enums.InsightLanguage;
import t1tanic.nutritionicu.model.enums.InsightType;

/**
 * A stored AI nutrition insight for a patient: the de-identified summary that was sent, the model's
 * answer, and a hash of the exact input. The hash lets us return a previous identical analysis instead
 * of paying for a fresh API call, and the history lets doctors compare earlier analyses and actions.
 */
@Entity
@Table(name = "ai_insight", indexes = {
        @Index(name = "idx_ai_insight_patient_hash", columnList = "patient_id, input_hash"),
        @Index(name = "idx_ai_insight_patient_content", columnList = "patient_id, content_hash"),
        @Index(name = "idx_ai_insight_patient_created", columnList = "patient_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class AiInsight extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** SHA-256 keying the exact (content + language) — the cache key for identical inputs. */
    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    /** SHA-256 of (model + knowledge + summary), language-independent — finds a source to translate. */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /** True when this insight was produced by translating an existing analysis rather than a fresh one. */
    @Column(name = "translated")
    private Boolean translated;

    @Column(name = "model", nullable = false, length = 64)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 8)
    private InsightLanguage language;

    /** Analysis vs comparison. Nullable for rows created before this column existed (treated as ANALYSIS). */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 16)
    private InsightType type;

    @Column(name = "deidentified_summary", nullable = false, columnDefinition = "text")
    private String deidentifiedSummary;

    @Column(name = "markdown", nullable = false, columnDefinition = "text")
    private String markdown;

    /** Reference document names this answer was grounded on, newline-separated; may be blank. */
    @Column(name = "knowledge_sources", columnDefinition = "text")
    private String knowledgeSources;
}
