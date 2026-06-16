package t1tanic.nutritionicu.dto;

import java.time.Instant;

/**
 * Flat, UI-friendly view of an alert — resolved inside a transaction so the
 * dashboard never touches lazy associations.
 */
public record AlertSummary(
        Long id,
        String severity,
        String status,
        String patientMrn,
        String sectors,
        String message,
        Instant createdAt) {
}
