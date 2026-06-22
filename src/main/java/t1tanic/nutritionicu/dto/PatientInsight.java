package t1tanic.nutritionicu.dto;

import java.time.Instant;
import java.util.List;
import t1tanic.nutritionicu.model.enums.InsightLanguage;
import t1tanic.nutritionicu.model.enums.InsightType;

/**
 * A patient AI insight: the exact (de-identified) summary that was sent, the model's markdown answer,
 * the language it is written in, which model produced it, and the reference documents it was grounded
 * on. {@code cached} is true when this was retrieved from a previous identical analysis instead of a
 * fresh API call; {@code translated} is true when it was produced by translating an earlier analysis
 * rather than analysing afresh; {@code createdAt} is when the underlying record was generated.
 */
public record PatientInsight(Long id, Instant createdAt, boolean cached, boolean translated,
                             InsightType type, InsightLanguage language, String model,
                             String deidentifiedSummary, String markdown, List<KnowledgeRef> knowledgeSources) {
}
