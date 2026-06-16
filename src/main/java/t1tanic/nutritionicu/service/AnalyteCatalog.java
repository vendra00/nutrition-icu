package t1tanic.nutritionicu.service;

import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Maps a raw printed analyte label to a stable canonical code, so the same
 * measurement is grouped across reports regardless of how it was written
 * (e.g. {@code Pla-Glucosa} in biochemistry vs {@code vSan-Glucosa} in a blood gas).
 *
 * <p>Stub implementation: an in-memory exact-match table seeded with the analytes
 * most relevant to nutrition insights. Intended to move to a DB-backed catalog
 * (with synonyms/regex) as more report formats are ingested. Unknown labels return
 * {@code null} — they're still stored with their raw name, just not yet canonicalized.
 */
@Component
public class AnalyteCatalog {

    private static final Map<String, String> CODES = Map.ofEntries(
            // Substrates / metabolic
            Map.entry("Pla-Glucosa", "GLUCOSE"),
            Map.entry("vSan-Glucosa", "GLUCOSE"),
            Map.entry("Pla-Urea", "UREA"),
            Map.entry("Pla-Creatinini", "CREATININE"),
            Map.entry("vSan-Lactat", "LACTATE"),
            Map.entry("Pla-Bilirubina", "BILIRUBIN_TOTAL"),
            Map.entry("Pla-Bilirubina esterificada", "BILIRUBIN_DIRECT"),
            // Ions / electrolytes (nutrition-critical)
            Map.entry("Pla-Ió sodi", "SODIUM"),
            Map.entry("vSan-Plasma; ió Sodi", "SODIUM"),
            Map.entry("Pla-Ió potassi", "POTASSIUM"),
            Map.entry("vSan-Plasma; ió Potassi", "POTASSIUM"),
            Map.entry("vSan-Plasma; ió Clorur", "CHLORIDE"),
            Map.entry("Pla-Calci", "CALCIUM"),
            Map.entry("vSan-Ió calci (II)", "CALCIUM_IONIZED"),
            Map.entry("Pla-Magnesi", "MAGNESIUM"),
            Map.entry("Pla-Fosfat", "PHOSPHATE"),
            // Proteins
            Map.entry("Pla-Proteïna", "TOTAL_PROTEIN"),
            // Enzymes
            Map.entry("Pla-Aspartat-aminotransferasa", "AST"),
            Map.entry("Pla-Alanina-aminotransferasa", "ALT"),
            // Hematology
            Map.entry("Hemoglobina", "HEMOGLOBIN"),
            Map.entry("Hematòcrit", "HEMATOCRIT"),
            Map.entry("Leucòcits", "WBC"),
            Map.entry("Plaquetes", "PLATELETS"));

    /** Canonical code for a raw analyte label, or {@code null} if not yet mapped. */
    public String codeFor(String rawAnalyteName) {
        if (rawAnalyteName == null) {
            return null;
        }
        return CODES.get(rawAnalyteName.strip());
    }
}
