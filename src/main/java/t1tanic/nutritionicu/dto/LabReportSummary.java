package t1tanic.nutritionicu.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A flat, UI-friendly row for the Lab reports table — fully resolved by a constructor query so the view
 * never touches lazy associations (works with {@code open-in-view=false}).
 */
public record LabReportSummary(
        Long id,
        Long patientId,
        String orderNumber,
        String patientMrn,
        String patientName,
        String department,
        LocalDate reportDate,
        LocalDateTime receptionAt,
        int sectionCount,
        String sourceFilename,
        Instant ingestedAt) {
}
