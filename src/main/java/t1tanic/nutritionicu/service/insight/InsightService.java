package t1tanic.nutritionicu.service.insight;

import java.util.List;
import t1tanic.nutritionicu.dto.PatientInsight;
import t1tanic.nutritionicu.model.enums.InsightLanguage;

/**
 * AI-assisted nutrition insights. Assembles a de-identified clinical summary for a patient and asks
 * Claude for trends and guideline-aligned suggestions. Decision support only — never autonomous care.
 */
public interface InsightService {

    /** Whether an Anthropic API key is configured (otherwise the view shows a setup notice). */
    boolean isConfigured();

    /**
     * Returns the model's analysis for a patient in the given language. If an identical input (same
     * model, language, knowledge and de-identified summary) was analysed before, the stored result is
     * returned with no API call; otherwise a fresh analysis is generated and saved. Each language is
     * cached and stored separately.
     *
     * @throws IllegalArgumentException if the patient does not exist
     * @throws IllegalStateException    if the API is not configured or the call fails
     */
    PatientInsight analyze(Long patientId, InsightLanguage language);

    /**
     * Compares a patient against similar past patients (age, BMI, NUTRIC, SOFA) and returns the model's
     * read on the probable course. Saved and cached/translated exactly like {@link #analyze}.
     *
     * @throws IllegalArgumentException if the patient does not exist
     * @throws IllegalStateException    if no comparable patients are found, the API is not configured,
     *                                  or the call fails
     */
    PatientInsight compare(Long patientId, InsightLanguage language);

    /** A patient's previously saved insights (analyses and comparisons), newest first. */
    List<PatientInsight> history(Long patientId);

    /** The de-identified summary that would be sent for a patient, without calling the API. */
    String previewSummary(Long patientId);
}
