package t1tanic.nutritionicu.dto;

import java.time.Instant;
import java.util.List;

/**
 * Flat, UI-friendly view of an alert — resolved inside a transaction so the dashboard never touches lazy
 * associations. {@code message} is the one-line localized summary (used on the dashboard); {@code results}
 * is the same abnormal readings as structured rows for the alert-detail table.
 */
public record AlertSummary(
        Long id,
        String severity,
        String status,
        Long patientId,
        String patientMrn,
        String sectors,
        String message,
        Instant createdAt,
        boolean refeedingRisk,
        List<AlertResult> results) {

    /** One abnormal lab reading on an alert, pre-localized for display. */
    public record AlertResult(String analyte, String value, String flag) {
    }
}
