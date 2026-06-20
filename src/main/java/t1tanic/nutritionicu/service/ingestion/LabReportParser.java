package t1tanic.nutritionicu.service.ingestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import t1tanic.nutritionicu.dto.ParsedReport;
import t1tanic.nutritionicu.model.LabReport;
import t1tanic.nutritionicu.model.LabResult;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.ReportSection;
import t1tanic.nutritionicu.model.enums.ResultFlag;
import t1tanic.nutritionicu.model.enums.Sex;
import t1tanic.nutritionicu.model.enums.Unit;

/**
 * Parses the plain text of a Vall d'Hebron lab report into a {@link ParsedReport}.
 *
 * <p>Two report layouts are supported, detected from the document text:
 * <ul>
 *   <li><b>Classic</b> — a compact Catalan header ({@code Pacient:}, {@code Petició:}, …)
 *       and bare ALL-CAPS lines opening categories/sections.</li>
 *   <li><b>Modern</b> — a bilingual Catalan/Spanish header ({@code Nom i cognoms /
 *       Nombre y apellidos:}, …) with explicit {@code Laboratorio:} (category) and
 *       {@code Sección:} (section) labels.</li>
 * </ul>
 *
 * <p>Both share the same result-row grammar — {@code name [flag] value [unit] [low - high]} —
 * so {@link #parseResult} and the value/date converters are reused across layouts.
 */
@Slf4j
@Component
public class LabReportParser {

    /** Top-level headings. The dash markers don't reliably distinguish these from
     *  sub-sections (e.g. GASOS EN SANG), so we recognise categories by vocabulary. */
    private static final Set<String> KNOWN_CATEGORIES = Set.of(
            "HEMATOLOGIA", "BIOQUIMICA", "GASOS EN SANG", "COAGULACIÓ", "COAGULACIO",
            "MICROBIOLOGIA", "IMMUNOLOGIA", "SEROLOGIA", "HORMONES", "ORINA",
            "SEDIMENT D'ORINA", "GASOMETRIA", "MARCADORS TUMORALS", "PROTEÏNES ESPECÍFIQUES");

    /** A value token: optional &lt;/&gt; then a number (dot or comma decimals). */
    private static final Pattern VALUE = Pattern.compile("^[<>]?-?\\d[\\d.,]*$");

    /** An ALL-CAPS heading line (letters, digits, common punctuation). */
    private static final Pattern HEADING =
            Pattern.compile("^\\p{Lu}[\\p{Lu}0-9 .,'’()/+%·\\-]*$");

    private static final Set<String> FLAGS = Set.of("↑", "↓", "↑↑", "↓↓", "↑↓");

    private static final Pattern VALIDATED_BY =
            Pattern.compile("Resultats revisats i validats per:\\s*(.*)$");

    // Catalan dates come as "22 de juny de 2024" (consonant month) or
    // "4 d'agost de 2024" (vowel month, elided apostrophe). Accept both.
    private static final Pattern REPORT_DATE =
            Pattern.compile("Barcelona,\\s*\\p{L}+,\\s*(\\d{1,2})\\s+d(?:e\\s+|['’]\\s*)(\\p{L}+)\\s+de\\s+(\\d{4})");

    /** Header field labels, used to split "Label: value Label: value" lines. */
    private static final String LABELS =
            "Pacient|Petició|Edat|Naixement|Nascut el|Sexe|Referència|NHC|CIP|T\\.Sanitària"
                    + "|Sol·licitant|Centre|Recepció|Servei|Finalització|Núm Seg Soc|Numero Host"
                    + "|Data anàlisis|Data tancament";
    private static final Pattern HEADER_FIELD =
            Pattern.compile("(" + LABELS + ")\\s*:\\s*(.*?)\\s*(?=(?:" + LABELS + ")\\s*:|$)");

    // ---- Modern layout (bilingual Catalan/Spanish) -----------------------
    // Single-field labels matched anywhere in the header block. The Spanish half of
    // each "Català / Castellano:" label is used as the anchor — it's the most stable.
    private static final Pattern M_ORDER =
            Pattern.compile("Número de petició del laboratori / Número de petición:\\s*(\\d+)");
    private static final Pattern M_NAME = Pattern.compile("Nombre y apellidos:\\s*(.+)");
    private static final Pattern M_BIRTH_SEX =
            Pattern.compile("Fecha de nacimiento:\\s*(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+Sexe / Sexo:\\s*(\\S+)");
    // NHC appears as "NHC / Nº de 20907272" (first page) or "NHC / Nº de Historia Clínica : 20907272".
    private static final Pattern M_NHC = Pattern.compile("NHC[^\\d]{0,40}?(\\d{6,})");
    private static final Pattern M_CIP = Pattern.compile("CIP-AUT:\\s*([A-Za-z0-9]+)");
    private static final Pattern M_CENTER = Pattern.compile("Centro de salud:\\s*(.+)");
    private static final Pattern M_PHYSICIAN = Pattern.compile("Nombre del profesional:\\s*(.+)");
    private static final Pattern M_DEPARTMENT = Pattern.compile("Servicio solicitante:\\s*(.+)");
    private static final Pattern M_DATETIME = Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{2,4}\\s+\\d{1,2}:\\d{2}:\\d{2})");
    private static final Pattern M_RECEPTION =
            Pattern.compile("recepción de la petición:\\s*" + M_DATETIME.pattern());
    // The header signature carries seconds (e.g. 21:47:46); per-section ones don't,
    // so requiring seconds naturally targets the report-level finalization time.
    private static final Pattern M_SIGNATURE =
            Pattern.compile("Fecha y hora de firma:\\s*" + M_DATETIME.pattern());
    private static final Pattern M_VALIDATED_BY =
            Pattern.compile("Resultats revisats i validats per / Nombre del responsable:\\s*(.*)$");
    private static final String M_CATEGORY_LABEL = "Laboratorio:";
    private static final String M_SECTION_LABEL = "Sección:";

    /** Modern-layout structural lines (page headers, metadata, column titles) carrying no result data. */
    private static final List<String> MODERN_NOISE_PREFIXES = List.of(
            "Centre /", "Adreça /", "Municipi /", "País /", "Telèfon /", "Domicili", "Codi postal",
            "Provincia", "Tipus de document", "Informe de Laboratori", "Número de petició",
            "Data d'obtenció", "Data i hora", "Dades pacient", "Dades sol·licitant", "Servei de salut",
            "Centre de salut", "Servei sol·licitant", "Servei Responsable", "Responsables àrea",
            "Nom del professional", "Categoria professional", "Tipus de mostra", "Número de la mostra",
            "Nom i cognoms", "D.naixement", "NHC /", "CIP-", "Prova", "Determinación");

    private static final Map<String, Integer> CATALAN_MONTHS = Map.ofEntries(
            Map.entry("gener", 1), Map.entry("febrer", 2), Map.entry("març", 3),
            Map.entry("abril", 4), Map.entry("maig", 5), Map.entry("juny", 6),
            Map.entry("juliol", 7), Map.entry("agost", 8), Map.entry("setembre", 9),
            Map.entry("octubre", 10), Map.entry("novembre", 11), Map.entry("desembre", 12));

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d/M/yyyy");
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("d/M/yy");
    private static final DateTimeFormatter DATE_TIME = new DateTimeFormatterBuilder()
            .appendPattern("d/M/")
            .appendValueReduced(ChronoField.YEAR, 2, 4, 2000)
            .optionalStart().appendPattern(" H:mm:ss").optionalEnd()
            .toFormatter(Locale.ROOT);

    public ParsedReport parse(String sourceFilename, String text) {
        List<String> lines = text.lines().map(String::strip).toList();

        Patient patient = new Patient();
        LabReport report = new LabReport();
        report.setSourceFilename(sourceFilename);
        report.setPatient(patient);

        if (isModernLayout(text)) {
            int bodyStart = parseHeaderModern(lines, text, patient, report);
            parseBodyModern(lines, bodyStart, report);
        } else {
            int bodyStart = parseHeader(lines, patient, report);
            parseBody(lines, bodyStart, report);
        }
        stampObservedAt(report);

        return new ParsedReport(patient, report);
    }

    /** The modern layout is identified by its distinctive laboratory-order label. */
    private static boolean isModernLayout(String text) {
        return text.contains("Número de petició del laboratori");
    }

    /**
     * Copies the report's effective measurement time onto every result, so each
     * value carries its own timestamp for trend queries (finalization → reception → date).
     */
    private void stampObservedAt(LabReport report) {
        LocalDateTime observedAt = report.getFinalizationAt();
        if (observedAt == null) {
            observedAt = report.getReceptionAt();
        }
        if (observedAt == null && report.getReportDate() != null) {
            observedAt = report.getReportDate().atStartOfDay();
        }
        if (observedAt == null) {
            return;
        }
        for (ReportSection section : report.getSections()) {
            for (LabResult result : section.getResults()) {
                result.setObservedAt(observedAt);
            }
        }
    }

    // ---- Header -----------------------------------------------------------

    /** Parses the metadata block and returns the index where the result body starts. */
    private int parseHeader(List<String> lines, Patient patient, LabReport report) {
        Map<String, String> fields = new LinkedHashMap<>();
        int firstCategory = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isCategoryHeading(line)) {
                firstCategory = i;
                break;
            }
            Matcher m = HEADER_FIELD.matcher(line);
            while (m.find()) {
                String label = m.group(1);
                String value = m.group(2).strip();
                if (!value.isEmpty()) {
                    fields.putIfAbsent(label, value);
                }
            }
        }

        patient.setFullName(fields.get("Pacient"));
        patient.setMedicalRecordNumber(digits(fields.get("NHC")));
        patient.setSex(parseSex(fields.get("Sexe")));
        patient.setBirthDate(parseDate(firstNonNull(fields.get("Naixement"), fields.get("Nascut el"))));
        patient.setHealthCardId(firstNonNull(fields.get("CIP"), fields.get("T.Sanitària")));
        patient.setSocialSecurityNumber(digits(fields.get("Núm Seg Soc")));

        report.setOrderNumber(digits(fields.get("Petició")));
        report.setReference(fields.get("Referència"));
        report.setHostNumber(digits(fields.get("Numero Host")));
        report.setRequestingPhysician(fields.get("Sol·licitant"));
        report.setCenter(fields.get("Centre"));
        report.setDepartment(fields.get("Servei"));
        report.setReportDate(parseReportDate(String.join("\n", lines)));
        // Same concept, different labels across report formats — collapse to one field each.
        report.setReceptionAt(parseDateTime(firstNonNull(fields.get("Recepció"), fields.get("Data anàlisis"))));
        report.setFinalizationAt(parseDateTime(firstNonNull(fields.get("Finalització"), fields.get("Data tancament"))));
        report.setAgeYearsAtReport(resolveAge(fields.get("Edat"), patient.getBirthDate(), report));

        return firstCategory;
    }

    // ---- Body -------------------------------------------------------------

    private void parseBody(List<String> lines, int start, LabReport report) {
        String currentCategory = null;
        ReportSection currentSection = null;
        int sectionSeq = 0;
        int resultSeq = 0;

        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isNoise(line)) {
                continue;
            }

            Matcher validated = VALIDATED_BY.matcher(line);
            if (validated.find()) {
                if (currentSection != null) {
                    String by = validated.group(1).strip();
                    currentSection.setValidatedBy(by.isEmpty() ? null : by);
                }
                continue;
            }

            if (isHeading(line)) {
                if (isCategory(line, currentCategory)) {
                    currentCategory = line;
                    currentSection = null; // first result/section will open one
                } else {
                    currentSection = new ReportSection(currentCategory, line);
                    currentSection.setSequence(sectionSeq++);
                    report.addSection(currentSection);
                    resultSeq = 0;
                }
                continue;
            }

            LabResult result = parseResult(line);
            if (result != null) {
                if (currentSection == null) {
                    currentSection = new ReportSection(currentCategory, null);
                    currentSection.setSequence(sectionSeq++);
                    report.addSection(currentSection);
                    resultSeq = 0;
                }
                result.setSequence(resultSeq++);
                currentSection.addResult(result);
            }
        }
    }

    // ---- Modern layout ----------------------------------------------------

    /** Parses the bilingual header block and returns the index where the result body starts. */
    private int parseHeaderModern(List<String> lines, String text, Patient patient, LabReport report) {
        int bodyStart = indexOfModernCategory(lines);
        String header = String.join("\n", lines.subList(0, bodyStart));

        patient.setFullName(group(M_NAME, header));
        patient.setMedicalRecordNumber(group(M_NHC, header));
        patient.setHealthCardId(group(M_CIP, header));
        Matcher birthSex = M_BIRTH_SEX.matcher(header);
        if (birthSex.find()) {
            patient.setBirthDate(parseDate(birthSex.group(1)));
            patient.setSex(parseSex(birthSex.group(2)));
        }

        report.setOrderNumber(group(M_ORDER, header));
        report.setCenter(group(M_CENTER, header));
        report.setRequestingPhysician(group(M_PHYSICIAN, header));
        report.setDepartment(group(M_DEPARTMENT, header));
        report.setReceptionAt(parseDateTime(group(M_RECEPTION, header)));
        report.setFinalizationAt(parseDateTime(group(M_SIGNATURE, header)));
        report.setReportDate(parseReportDate(text));
        report.setAgeYearsAtReport(resolveAge(null, patient.getBirthDate(), report));

        return bodyStart;
    }

    private void parseBodyModern(List<String> lines, int start, LabReport report) {
        String currentCategory = null;
        ReportSection currentSection = null;
        int sectionSeq = 0;
        int resultSeq = 0;
        boolean expectSectionName = false;

        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isNoise(line)) {
                continue;
            }

            if (line.startsWith(M_CATEGORY_LABEL)) {
                currentCategory = line.substring(M_CATEGORY_LABEL.length()).strip();
                currentSection = null; // the section under it opens next
                expectSectionName = false;
                continue;
            }
            if (line.startsWith(M_SECTION_LABEL)) {
                // The section name usually sits on a following line; capture it inline if present.
                String inline = line.substring(M_SECTION_LABEL.length()).strip();
                if (inline.isEmpty()) {
                    expectSectionName = true;
                } else {
                    currentSection = openSection(report, currentCategory, inline, sectionSeq++);
                    resultSeq = 0;
                }
                continue;
            }

            Matcher validated = M_VALIDATED_BY.matcher(line);
            if (validated.find()) {
                if (currentSection != null) {
                    String by = validated.group(1).strip();
                    currentSection.setValidatedBy(by.isEmpty() ? null : by);
                }
                expectSectionName = false;
                continue;
            }

            if (expectSectionName) {
                currentSection = openSection(report, currentCategory, line, sectionSeq++);
                resultSeq = 0;
                expectSectionName = false;
                continue;
            }

            if (isNonResultText(line)) {
                continue;
            }
            LabResult result = parseResult(line);
            if (result != null && currentSection != null) {
                result.setSequence(resultSeq++);
                currentSection.addResult(result);
            }
        }
    }

    private ReportSection openSection(LabReport report, String category, String name, int sequence) {
        ReportSection section = new ReportSection(category, name);
        section.setSequence(sequence);
        report.addSection(section);
        return section;
    }

    /**
     * Explanatory text that some analytes carry but which isn't a measurement — checked only
     * after the structural (category/section/validated) lines have been handled, so the colon
     * rule below is safe. Covers:
     * <ul>
     *   <li>decision-limit notes, e.g. "Població adulta &lt; 200 mg/dL" or "2-9 anys &gt;= 100 mg/dL"
     *       (a space-delimited comparison operator; a real result attaches it, e.g. {@code >90});</li>
     *   <li>prose footnotes/references, e.g. the CKD-EPI eGFR staging notes
     *       ("El valor estimat de FG…", "En persones de raça negra…pel factor: 1.159") — these
     *       contain a colon, which real result rows never do.</li>
     * </ul>
     */
    private static boolean isNonResultText(String line) {
        return line.startsWith("Població")
                || line.contains(":")
                || line.matches(".*\\s[<>]=?\\s.*");
    }

    private static int indexOfModernCategory(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(M_CATEGORY_LABEL)) {
                return i;
            }
        }
        return lines.size();
    }

    /** First capturing group of the first match, stripped, or null if absent/blank. */
    private static String group(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) {
            return null;
        }
        String value = m.group(1).strip();
        return value.isEmpty() ? null : value;
    }

    /** Parses one result row, or returns null if the line isn't a measurement. */
    private LabResult parseResult(String line) {
        String[] tokens = line.split("\\s+");
        int valueIdx = -1;
        for (int i = 1; i < tokens.length; i++) { // i>=1: need at least one name token
            if (VALUE.matcher(tokens[i]).matches()) {
                valueIdx = i;
                break;
            }
        }
        if (valueIdx < 0) {
            return null;
        }

        int nameEnd = valueIdx;
        ResultFlag flag = ResultFlag.NORMAL;
        if (FLAGS.contains(tokens[valueIdx - 1])) {
            flag = toFlag(tokens[valueIdx - 1]);
            nameEnd = valueIdx - 1;
        }
        String name = String.join(" ", Arrays.copyOfRange(tokens, 0, nameEnd)).strip();
        // A real analyte name always contains letters; this rejects stray table rows like
        // "5 <15 Fallida renal" (from the eGFR staging footnote) that otherwise look numeric.
        if (name.chars().noneMatch(Character::isLetter)) {
            return null;
        }

        String valueRaw = tokens[valueIdx];
        String[] rest = Arrays.copyOfRange(tokens, valueIdx + 1, tokens.length);

        LabResult result = new LabResult();
        result.setAnalyteName(name);
        result.setFlag(flag);
        result.setValueRaw(valueRaw);
        if (valueRaw.startsWith("<") || valueRaw.startsWith(">")) {
            result.setValueOperator(valueRaw.substring(0, 1));
        }
        result.setValueNumeric(toDecimal(valueRaw));

        // Trailing "low - high" reference range, if present.
        if (rest.length >= 3
                && rest[rest.length - 2].equals("-")
                && VALUE.matcher(rest[rest.length - 1]).matches()
                && VALUE.matcher(rest[rest.length - 3]).matches()) {
            result.setRefLow(toDecimal(rest[rest.length - 3]));
            result.setRefHigh(toDecimal(rest[rest.length - 1]));
            result.setRefRaw(rest[rest.length - 3] + " - " + rest[rest.length - 1]);
            rest = Arrays.copyOfRange(rest, 0, rest.length - 3);
        }
        String unitText = String.join(" ", rest).strip();
        if (!unitText.isEmpty()) {
            result.setUnitRaw(unitText);
            Optional<Unit> unit = Unit.fromRaw(unitText);
            unit.ifPresentOrElse(result::setUnit,
                    () -> log.debug("Unrecognized unit '{}' for analyte '{}'", unitText, name));
        }
        return result;
    }

    // ---- Classification helpers ------------------------------------------

    private boolean isHeading(String line) {
        if (!HEADING.matcher(line).matches()) {
            return false;
        }
        long letters = line.chars().filter(Character::isLetter).count();
        return letters >= 3;
    }

    private boolean isCategoryHeading(String line) {
        return isHeading(line) && KNOWN_CATEGORIES.contains(line);
    }

    /** A heading is a category if it's a known one, or if no category is open yet. */
    private boolean isCategory(String line, String currentCategory) {
        return KNOWN_CATEGORIES.contains(line) || currentCategory == null;
    }

    /** Structural/decoration lines carrying no data. */
    private boolean isNoise(String line) {
        if (line.isEmpty()) {
            return true;
        }
        // Only dots, the letter 'b', dashes, underscores and spaces.
        if (line.matches("[.\\-_ b]+")) {
            return true;
        }
        if (line.matches("b-{2,}.*")) {
            return true;
        }
        if (line.startsWith("Pàgina ") || line.startsWith("Laboratoris Clínics")
                || line.startsWith("Pacient:") || line.startsWith("Recepció:")
                || line.startsWith("NHC:") || line.startsWith("Incidències")
                || line.startsWith("Barcelona,") || line.startsWith("Passeig")
                || line.startsWith("Els Laboratoris") || line.startsWith("Catàleg")
                || line.startsWith("Les proves") || line.startsWith("Núm.")
                || line.equals("blanco") || line.startsWith("cc ")
                || line.startsWith("www.") || line.startsWith("http")) {
            return true;
        }
        return MODERN_NOISE_PREFIXES.stream().anyMatch(line::startsWith);
    }

    // ---- Value / type conversion -----------------------------------------

    private static ResultFlag toFlag(String arrow) {
        return switch (arrow) {
            case "↑" -> ResultFlag.HIGH;
            case "↑↑" -> ResultFlag.VERY_HIGH;
            case "↓" -> ResultFlag.LOW;
            case "↓↓" -> ResultFlag.VERY_LOW;
            default -> ResultFlag.NORMAL;
        };
    }

    private static Sex parseSex(String raw) {
        if (raw == null || raw.isBlank()) {
            return Sex.UNKNOWN;
        }
        return switch (Character.toUpperCase(raw.strip().charAt(0))) {
            case 'M' -> Sex.MALE;
            case 'F', 'D' -> Sex.FEMALE; // D = Dona (female, Catalan)
            default -> Sex.UNKNOWN;
        };
    }

    private static BigDecimal toDecimal(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replace("<", "").replace(">", "").replace(",", ".").strip();
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String digits(String raw) {
        if (raw == null) {
            return null;
        }
        String d = raw.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }

    private static Integer parseInt(String raw) {
        String d = digits(raw);
        return d == null ? null : Integer.valueOf(d);
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String token = raw.strip().split("\\s+")[0];
        for (DateTimeFormatter f : List.of(DATE, DATE_SHORT)) {
            try {
                return LocalDate.parse(token, f);
            } catch (Exception ignored) {
                // try next format
            }
        }
        return null;
    }

    private static LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.strip();
        try {
            return LocalDateTime.parse(value, DATE_TIME);
        } catch (Exception ignored) {
            LocalDate d = parseDate(value);
            return d == null ? null : d.atStartOfDay();
        }
    }

    private LocalDate parseReportDate(String text) {
        Matcher m = REPORT_DATE.matcher(text);
        if (!m.find()) {
            return null;
        }
        Integer month = CATALAN_MONTHS.get(m.group(2).toLowerCase(Locale.ROOT));
        if (month == null) {
            return null;
        }
        try {
            return LocalDate.of(Integer.parseInt(m.group(3)), month, Integer.parseInt(m.group(1)));
        } catch (Exception e) {
            log.debug("Could not build report date from '{}'", m.group(), e);
            return null;
        }
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    /**
     * Uses the printed "Edat" when present; otherwise derives age from the date of
     * birth against the report's own date (report date, else reception/finalization).
     */
    private static Integer resolveAge(String edat, LocalDate birthDate, LabReport report) {
        Integer printed = parseInt(edat);
        if (printed != null) {
            return printed;
        }
        if (birthDate == null) {
            return null;
        }
        LocalDate reference = report.getReportDate();
        if (reference == null && report.getReceptionAt() != null) {
            reference = report.getReceptionAt().toLocalDate();
        }
        if (reference == null && report.getFinalizationAt() != null) {
            reference = report.getFinalizationAt().toLocalDate();
        }
        if (reference == null) {
            return null;
        }
        return Period.between(birthDate, reference).getYears();
    }
}
