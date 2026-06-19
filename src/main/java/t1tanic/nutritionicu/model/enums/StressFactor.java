package t1tanic.nutritionicu.model.enums;

/**
 * Clinical stress degree applied to the Harris-Benedict basal expenditure, mirroring the
 * <a href="https://www.rccc.eu/nutri/HB.html">rccc.eu</a> calculator's "Grado de Estrés" list.
 *
 * <p>{@link #factor()} is the multiplier from that table; total expenditure adds a fixed 0.1
 * activity term on top (GET = GEB × (0.1 + factor)), reproducing the source calculator exactly.
 */
public enum StressFactor {
    NO_STRESS("No stress", 1.0),
    MALNUTRITION("Malnutrition", 0.85),
    CONTROLLED_INFECTION("Controlled infection", 1.2),
    UNCOMPLICATED_SURGERY("Uncomplicated surgery", 1.2),
    POSTOPERATIVE("Postoperative", 1.05),
    SEVERE_SEPSIS("Severe sepsis", 1.45),
    COMPLICATED_SURGERY("Complicated surgery", 1.5),
    PANCREATITIS("Pancreatitis", 1.5),
    BURNS_UNDER_20("Burns, <20%", 1.4),
    BURNS_20_TO_40("Burns, 20–40%", 1.6),
    BURNS_OVER_40("Burns, >40%", 1.9),
    SEVERE_TRAUMA("Severe trauma", 1.3),
    SEVERE_TBI("Severe TBI", 1.5),
    OTHER_TRAUMA("Other trauma", 1.2),
    CANCER("Cancer", 1.5);

    private final String label;
    private final double factor;

    StressFactor(String label, double factor) {
        this.label = label;
        this.factor = factor;
    }

    public String label() {
        return label;
    }

    public double factor() {
        return factor;
    }
}
