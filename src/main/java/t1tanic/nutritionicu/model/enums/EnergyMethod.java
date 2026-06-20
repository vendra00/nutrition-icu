package t1tanic.nutritionicu.model.enums;

/** How a patient's energy expenditure was determined for a stored {@code EnergyAssessment}. */
public enum EnergyMethod {
    HARRIS_BENEDICT("Harris-Benedict"),
    INDIRECT_CALORIMETRY("Indirect calorimetry");

    private final String label;

    EnergyMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
