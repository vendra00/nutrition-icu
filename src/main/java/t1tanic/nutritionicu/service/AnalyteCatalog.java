package t1tanic.nutritionicu.service;

import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Maps a raw printed (Catalan) analyte label to canonical info: a stable {@code code}
 * for grouping/routing, and an {@code englishName} for display. Same analyte written
 * differently across report types maps to one code (e.g. {@code Pla-Glucosa} and
 * {@code vSan-Glucosa} both → GLUCOSE).
 *
 * <p>Stub implementation: an in-memory exact-match table seeded from the reports seen
 * so far. Intended to move to a DB-backed catalog (synonyms/patterns) over time.
 * Unknown labels return {@code null} code and fall back to the raw text for display.
 */
@Component
public class AnalyteCatalog {

    /** Canonical info for a raw analyte label. */
    public record CanonicalAnalyte(String code, String englishName) {
    }

    private static final Map<String, CanonicalAnalyte> CATALOG = Map.ofEntries(
            // Hematology — hemogram
            Map.entry("Hematies", new CanonicalAnalyte("RBC", "Red blood cells (RBC)")),
            Map.entry("Hemoglobina", new CanonicalAnalyte("HEMOGLOBIN", "Hemoglobin")),
            Map.entry("Hematòcrit", new CanonicalAnalyte("HEMATOCRIT", "Hematocrit")),
            Map.entry("Volum corpuscular mig (VCM)", new CanonicalAnalyte("MCV", "Mean corpuscular volume (MCV)")),
            Map.entry("Hemoglobina corpuscular mitja (HCM)", new CanonicalAnalyte("MCH", "Mean corpuscular hemoglobin (MCH)")),
            Map.entry("Concentració HGB Corpuscular mitja", new CanonicalAnalyte("MCHC", "Mean corpuscular Hb conc. (MCHC)")),
            Map.entry("Ample Distribució Eritrocits (ADE)", new CanonicalAnalyte("RDW", "Red cell distribution width (RDW)")),
            Map.entry("San-Eritroblastes, f", new CanonicalAnalyte("NRBC_PCT", "Nucleated RBC %")),
            Map.entry("San-Eritroblastes, c", new CanonicalAnalyte("NRBC_ABS", "Nucleated RBC (count)")),
            Map.entry("Leucòcits", new CanonicalAnalyte("WBC", "White blood cells (WBC)")),
            Map.entry("Neutròfils %", new CanonicalAnalyte("NEUTROPHILS_PCT", "Neutrophils %")),
            Map.entry("Limfòcits %", new CanonicalAnalyte("LYMPHOCYTES_PCT", "Lymphocytes %")),
            Map.entry("Monòcits %", new CanonicalAnalyte("MONOCYTES_PCT", "Monocytes %")),
            Map.entry("Eosinòfils %", new CanonicalAnalyte("EOSINOPHILS_PCT", "Eosinophils %")),
            Map.entry("Basòfils %", new CanonicalAnalyte("BASOPHILS_PCT", "Basophils %")),
            Map.entry("Neutròfils", new CanonicalAnalyte("NEUTROPHILS_ABS", "Neutrophils")),
            Map.entry("Limfòcits", new CanonicalAnalyte("LYMPHOCYTES_ABS", "Lymphocytes")),
            Map.entry("Monòcits", new CanonicalAnalyte("MONOCYTES_ABS", "Monocytes")),
            Map.entry("Eosinòfils", new CanonicalAnalyte("EOSINOPHILS_ABS", "Eosinophils")),
            Map.entry("Basòfils", new CanonicalAnalyte("BASOPHILS_ABS", "Basophils")),
            Map.entry("Plaquetes", new CanonicalAnalyte("PLATELETS", "Platelets")),
            Map.entry("Volum plaquetari mig", new CanonicalAnalyte("MPV", "Mean platelet volume (MPV)")),
            // Hemostasis
            Map.entry("Pla-Temps de Protrombina (ràtio)", new CanonicalAnalyte("PT_RATIO", "Prothrombin time (ratio)")),
            Map.entry("Pla-Temps de Protrombina (%)", new CanonicalAnalyte("PT_PCT", "Prothrombin time (%)")),
            Map.entry("Pla-Temps de Protrombina (INR)", new CanonicalAnalyte("INR", "INR")),
            Map.entry("Pla-Temps de Protrombina (s)", new CanonicalAnalyte("PT_SEC", "Prothrombin time (s)")),
            Map.entry("Pla-TTPA (ràtio)", new CanonicalAnalyte("APTT_RATIO", "aPTT (ratio)")),
            Map.entry("Pla-TTPA (s)", new CanonicalAnalyte("APTT_SEC", "aPTT (s)")),
            Map.entry("Pla-Fibrinogen derivat", new CanonicalAnalyte("FIBRINOGEN", "Fibrinogen (derived)")),
            Map.entry("Pla-Dimer D (ng/ml)", new CanonicalAnalyte("D_DIMER", "D-dimer")),
            Map.entry("Pla-Temps de Trombina (ràtio)", new CanonicalAnalyte("TT_RATIO", "Thrombin time (ratio)")),
            Map.entry("Pla-Temps de Trombina (s)", new CanonicalAnalyte("TT_SEC", "Thrombin time (s)")),
            // Biochemistry — substrates
            Map.entry("Pla-Glucosa", new CanonicalAnalyte("GLUCOSE", "Glucose")),
            Map.entry("Pla-Urea", new CanonicalAnalyte("UREA", "Urea")),
            Map.entry("Pla-Creatinini", new CanonicalAnalyte("CREATININE", "Creatinine")),
            Map.entry("Pac-Filtrat glomerular (estimació segons CKD-EPI)", new CanonicalAnalyte("EGFR", "eGFR (CKD-EPI)")),
            Map.entry("Pla-Bilirubina", new CanonicalAnalyte("BILIRUBIN_TOTAL", "Bilirubin (total)")),
            Map.entry("Pla-Bilirubina esterificada", new CanonicalAnalyte("BILIRUBIN_DIRECT", "Bilirubin (direct)")),
            // Ions
            Map.entry("Pla-Ió sodi", new CanonicalAnalyte("SODIUM", "Sodium")),
            Map.entry("Pla-Ió potassi", new CanonicalAnalyte("POTASSIUM", "Potassium")),
            Map.entry("Pla-Fosfat", new CanonicalAnalyte("PHOSPHATE", "Phosphate")),
            Map.entry("Pla-Magnesi", new CanonicalAnalyte("MAGNESIUM", "Magnesium")),
            Map.entry("Pla-Calci", new CanonicalAnalyte("CALCIUM", "Calcium")),
            // Enzymes
            Map.entry("Pla-Aspartat-aminotransferasa", new CanonicalAnalyte("AST", "AST")),
            Map.entry("Pla-Alanina-aminotransferasa", new CanonicalAnalyte("ALT", "ALT")),
            Map.entry("Pla-Creatina-cinasa", new CanonicalAnalyte("CK", "Creatine kinase (CK)")),
            // Proteins
            Map.entry("Pla-Proteïna", new CanonicalAnalyte("TOTAL_PROTEIN", "Total protein")),
            Map.entry("Pla-Albúmina", new CanonicalAnalyte("ALBUMIN", "Albumin")),
            Map.entry("Pla-Proteïna C reactiva", new CanonicalAnalyte("CRP", "C-reactive protein (CRP)")),
            // Inflammation / metabolic-phase markers (Ebb vs Flow)
            Map.entry("Pla-Procalcitonina", new CanonicalAnalyte("PROCALCITONIN", "Procalcitonin (PCT)")),
            Map.entry("Srm-Procalcitonina", new CanonicalAnalyte("PROCALCITONIN", "Procalcitonin (PCT)")),
            // Cardiac markers
            Map.entry("Pla-Troponina I alta sensibilitat", new CanonicalAnalyte("TROPONIN_I_HS", "Troponin I (high-sensitivity)")),
            Map.entry("Pla-NT-Pro-Pèptid natriurètic cerebral", new CanonicalAnalyte("NT_PROBNP", "NT-proBNP")),
            // Serum biochemistry — modern reports label serum "Srm-" where classic used plasma "Pla-".
            Map.entry("Srm-Glucosa", new CanonicalAnalyte("GLUCOSE", "Glucose")),
            Map.entry("Srm-Urea", new CanonicalAnalyte("UREA", "Urea")),
            Map.entry("Srm-Creatinini", new CanonicalAnalyte("CREATININE", "Creatinine")),
            Map.entry("Srm-Bilirubina", new CanonicalAnalyte("BILIRUBIN_TOTAL", "Bilirubin (total)")),
            Map.entry("Srm-Bilirubina esterificada", new CanonicalAnalyte("BILIRUBIN_DIRECT", "Bilirubin (direct)")),
            Map.entry("Srm-Ió sodi", new CanonicalAnalyte("SODIUM", "Sodium")),
            Map.entry("Srm-Ió potassi", new CanonicalAnalyte("POTASSIUM", "Potassium")),
            Map.entry("Srm-Fosfat", new CanonicalAnalyte("PHOSPHATE", "Phosphate")),
            Map.entry("Srm-Magnesi", new CanonicalAnalyte("MAGNESIUM", "Magnesium")),
            Map.entry("Srm-Calci", new CanonicalAnalyte("CALCIUM", "Calcium")),
            Map.entry("Srm-Calci corregit per l'albúmina", new CanonicalAnalyte("CALCIUM_CORRECTED", "Calcium (albumin-corrected)")),
            Map.entry("Srm-Aspartat-aminotransferasa", new CanonicalAnalyte("AST", "AST")),
            Map.entry("Srm-Alanina-aminotransferasa", new CanonicalAnalyte("ALT", "ALT")),
            Map.entry("Srm-Fosfatasa alcalina", new CanonicalAnalyte("ALP", "Alkaline phosphatase (ALP)")),
            Map.entry("Srm-Gamma-glutamiltransferasa", new CanonicalAnalyte("GGT", "Gamma-glutamyl transferase (GGT)")),
            Map.entry("Srm-Proteïna", new CanonicalAnalyte("TOTAL_PROTEIN", "Total protein")),
            Map.entry("Srm-Proteïna C reactiva", new CanonicalAnalyte("CRP", "C-reactive protein (CRP)")),
            Map.entry("Srm-Albúmina", new CanonicalAnalyte("ALBUMIN", "Albumin")),
            Map.entry("Srm-Transtiretina (prealbumina)", new CanonicalAnalyte("PREALBUMIN", "Prealbumin (transthyretin)")),
            Map.entry("Srm-Colesterol", new CanonicalAnalyte("CHOLESTEROL", "Cholesterol (total)")),
            Map.entry("Srm-Triglicèrid", new CanonicalAnalyte("TRIGLYCERIDES", "Triglycerides")),
            Map.entry("Srm-Urat", new CanonicalAnalyte("URATE", "Urate (uric acid)")),
            Map.entry("Srm-Alfa-amilasa pancreàtica", new CanonicalAnalyte("PANCREATIC_AMYLASE", "Pancreatic amylase")),
            // Blood gas (POCT)
            Map.entry("vSan-Plasma; ió Sodi", new CanonicalAnalyte("SODIUM", "Sodium (blood gas)")),
            Map.entry("vSan-Plasma; ió Potassi", new CanonicalAnalyte("POTASSIUM", "Potassium (blood gas)")),
            Map.entry("vSan-Plasma; ió Clorur", new CanonicalAnalyte("CHLORIDE", "Chloride")),
            Map.entry("vSan-Ió calci (II)", new CanonicalAnalyte("CALCIUM_IONIZED", "Calcium (II)")),
            Map.entry("Calci iònic-Sang venosa pH=7.40 (37ºC)", new CanonicalAnalyte("CALCIUM_IONIZED", "Ionized calcium (pH 7.40)")),
            Map.entry("vSan-Plasma; pH", new CanonicalAnalyte("PH_VENOUS", "pH (venous)")),
            Map.entry("vSan-Diòxid de carboni (lliure); tensió", new CanonicalAnalyte("PCO2", "pCO2 (venous)")),
            Map.entry("vSan-Oxigen; tensió", new CanonicalAnalyte("PO2", "pO2 (venous)")),
            Map.entry("vSan-Hidrogencarbonat; c.subst.(actual)", new CanonicalAnalyte("HCO3_ACTUAL", "Bicarbonate (actual)")),
            Map.entry("vSan-Diòxid de carboni (total); c.subst", new CanonicalAnalyte("TCO2", "Total CO2")),
            Map.entry("vSan-Excés de base(llocs enllaçants d'H+); c.subst.", new CanonicalAnalyte("BASE_EXCESS_BLOOD", "Base excess (blood)")),
            Map.entry("vSan-Excés de base extracelular; c.subst.", new CanonicalAnalyte("BASE_EXCESS_ECF", "Base excess (extracellular)")),
            Map.entry("vSan-Hidrogencarbonat; c.subst.(estandar)", new CanonicalAnalyte("HCO3_STANDARD", "Bicarbonate (standard)")),
            Map.entry("Hb(vSan)-Oxigen; fr.sat.", new CanonicalAnalyte("O2_SAT", "Oxygen saturation")),
            Map.entry("vSan-Glucosa", new CanonicalAnalyte("GLUCOSE", "Glucose (blood gas)")),
            Map.entry("vSan-Lactat", new CanonicalAnalyte("LACTATE", "Lactate")),
            Map.entry("Ven-Hemoglobina total", new CanonicalAnalyte("HEMOGLOBIN", "Total hemoglobin")));

    /** Canonical code for a raw analyte label, or {@code null} if not yet mapped. */
    public String codeFor(String rawAnalyteName) {
        CanonicalAnalyte analyte = lookup(rawAnalyteName);
        return analyte == null ? null : analyte.code();
    }

    /** English display name for a raw label, falling back to the raw text if unmapped. */
    public String displayName(String rawAnalyteName) {
        CanonicalAnalyte analyte = lookup(rawAnalyteName);
        return analyte != null ? analyte.englishName() : rawAnalyteName;
    }

    private CanonicalAnalyte lookup(String rawAnalyteName) {
        return rawAnalyteName == null ? null : CATALOG.get(rawAnalyteName.strip());
    }
}
