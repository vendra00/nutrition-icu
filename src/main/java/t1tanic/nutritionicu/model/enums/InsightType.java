package t1tanic.nutritionicu.model.enums;

/** What kind of AI insight a stored record is. */
public enum InsightType {

    /** Analysis of a single patient's own data. */
    ANALYSIS("Analysis"),

    /** Comparison of a patient against similar past patients, with the probable course. */
    COMPARISON("Comparison");

    private final String label;

    InsightType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
