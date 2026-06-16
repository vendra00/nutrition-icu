package t1tanic.nutritionicu.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import t1tanic.nutritionicu.dto.ParsedReport;
import t1tanic.nutritionicu.model.LabReport;
import t1tanic.nutritionicu.model.LabResult;
import t1tanic.nutritionicu.model.ReportSection;
import t1tanic.nutritionicu.model.enums.ResultFlag;

/** Verifies the parser against the real PDFBox text of the two sample reports. */
class LabReportParserTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();
    private final LabReportParser parser = new LabReportParser();

    private ParsedReport parse(String filename) {
        Path pdf = Path.of("src/test/resources/samples", filename);
        return parser.parse(filename, extractor.extract(pdf));
    }

    private void dump(ParsedReport parsed) {
        LabReport r = parsed.report();
        System.out.println("== " + r.getSourceFilename() + " ==");
        System.out.printf("Patient: %s | NHC=%s | sex=%s | born=%s%n",
                parsed.patient().getFullName(), parsed.patient().getMedicalRecordNumber(),
                parsed.patient().getSex(), parsed.patient().getBirthDate());
        System.out.printf("order=%s | department=%s | center=%s | requestedBy=%s%n",
                r.getOrderNumber(), r.getDepartment(), r.getCenter(), r.getRequestingPhysician());
        for (ReportSection s : r.getSections()) {
            System.out.printf("  [%s / %s] validatedBy=%s%n", s.getCategory(), s.getName(), s.getValidatedBy());
            for (LabResult x : s.getResults()) {
                System.out.printf("      %-45s %s %-8s %-12s ref=%s%n",
                        x.getAnalyteName(), x.getFlag() == ResultFlag.NORMAL ? " " : x.getFlag(),
                        x.getValueRaw(), x.getUnit() == null ? "" : x.getUnit(), x.getRefRaw());
            }
        }
    }

    @Test
    void parsesFullPanelReport() {
        ParsedReport parsed = parse("MED10098888631_20240622_223541.PDF");
        dump(parsed);

        assertThat(parsed.patient().getMedicalRecordNumber()).isEqualTo("18823834");
        assertThat(parsed.patient().getFullName()).contains("BOUSSOURA");
        assertThat(parsed.report().getOrderNumber()).isEqualTo("102370048");
        assertThat(parsed.report().getDepartment()).isEqualTo("NEUROLOGIA");
        // "22 de juny de 2024" — consonant-month Catalan date.
        assertThat(parsed.report().getReportDate()).isEqualTo(LocalDate.of(2024, 6, 22));
        // Full-panel labels Recepció / Finalització.
        assertThat(parsed.report().getReceptionAt()).isEqualTo(LocalDateTime.of(2024, 6, 22, 21, 43, 28));
        assertThat(parsed.report().getFinalizationAt()).isEqualTo(LocalDateTime.of(2024, 6, 22, 22, 27, 16));
        // "Edat: 50 anys" printed directly.
        assertThat(parsed.report().getAgeYearsAtReport()).isEqualTo(50);

        // Categories present, results bucketed under sections.
        assertThat(parsed.report().getSections())
                .extracting(ReportSection::getCategory)
                .contains("HEMATOLOGIA", "BIOQUIMICA");

        LabResult sodi = findResult(parsed, "Pla-Ió sodi");
        assertThat(sodi.getValueNumeric()).isEqualByComparingTo("138.0");
        assertThat(sodi.getUnit()).isEqualTo("mmol/L");
        assertThat(sodi.getRefLow()).isEqualByComparingTo("136.0");
        assertThat(sodi.getRefHigh()).isEqualByComparingTo("146.0");

        LabResult glucosa = findResult(parsed, "Pla-Glucosa");
        assertThat(glucosa.getFlag()).isEqualTo(ResultFlag.HIGH);
        assertThat(glucosa.getValueNumeric()).isEqualByComparingTo("124");

        // Each result carries the report's measurement time for trend queries.
        LabResult plaquetes = findResult(parsed, "Plaquetes");
        assertThat(plaquetes.getValueNumeric()).isEqualByComparingTo("216");
        assertThat(plaquetes.getObservedAt()).isEqualTo(LocalDateTime.of(2024, 6, 22, 22, 27, 16));
    }

    @Test
    void parsesPoctReport() {
        ParsedReport parsed = parse("MED10099768533_20240804_064536.PDF");
        dump(parsed);

        assertThat(parsed.report().getDepartment()).isEqualTo("POCT-UCID1");
        // "4 d'agost de 2024" — vowel-month elided Catalan date.
        assertThat(parsed.report().getReportDate()).isEqualTo(LocalDate.of(2024, 8, 4));
        // POCT labels Data anàlisis / Data tancament map to the same fields.
        assertThat(parsed.report().getReceptionAt()).isEqualTo(LocalDateTime.of(2024, 8, 4, 6, 32, 4));
        assertThat(parsed.report().getFinalizationAt()).isEqualTo(LocalDateTime.of(2024, 8, 4, 6, 32, 33));
        // No "Edat" printed → computed from DOB (1973-10-29) vs report date 2024-08-04.
        assertThat(parsed.report().getAgeYearsAtReport()).isEqualTo(50);
        assertThat(parsed.report().getSections())
                .extracting(ReportSection::getCategory)
                .contains("BIOQUIMICA", "GASOS EN SANG");

        // "<3"-style and operator values handled.
        LabResult ph = findResult(parsed, "vSan-Plasma; pH");
        assertThat(ph.getFlag()).isEqualTo(ResultFlag.VERY_HIGH);
        assertThat(ph.getValueNumeric()).isEqualByComparingTo("7.46");
        assertThat(ph.getUnit()).isNull();

        LabResult glucosa = findResult(parsed, "vSan-Glucosa");
        assertThat(glucosa.getFlag()).isEqualTo(ResultFlag.HIGH);
        assertThat(glucosa.getValueNumeric()).isEqualByComparingTo("130");
    }

    private LabResult findResult(ParsedReport parsed, String analyte) {
        return parsed.report().getSections().stream()
                .flatMap(s -> s.getResults().stream())
                .filter(x -> x.getAnalyteName().equals(analyte))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Result not found: " + analyte));
    }
}
