package t1tanic.nutritionicu.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The full contents of one lab report, fully resolved inside a transaction so the detail view never touches
 * lazy associations (works with {@code open-in-view=false}). Mirrors the PDF layout: report-level header
 * fields plus the ordered sections, each with its rows of measured analytes.
 */
public record LabReportDetail(
        Long patientId,
        String patientMrn,
        String patientName,
        String orderNumber,
        String reference,
        String department,
        String center,
        String requestingPhysician,
        LocalDate reportDate,
        LocalDateTime receptionAt,
        LocalDateTime finalizationAt,
        Integer ageYearsAtReport,
        String sourceFilename,
        List<Section> sections) {

    /** One block within the report (HEMATOLOGIA › HEMOGRAMA …) and its measured rows. */
    public record Section(String category, String name, String validatedBy, List<Row> rows) {
    }

    /**
     * One measured analyte. {@code code} is the canonical analyte code (nullable) the view localizes via
     * {@code analyte.code.<CODE>}; {@code analyte} is the fallback display name (catalog English, else the raw
     * printed label) used when there is no code. The rest is as printed: value, unit, flag, reference range.
     */
    public record Row(String code, String analyte, String value, String unit, String flag, String reference) {
    }
}
