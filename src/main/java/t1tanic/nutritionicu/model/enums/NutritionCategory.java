package t1tanic.nutritionicu.model.enums;

/** Route of the nutrition formula offered in the rccc.eu "Elige Nutrición" step. */
public enum NutritionCategory {
    ENTERAL("Enteral"),
    PARENTERAL("Parenteral"),
    SUPPLEMENT("Supplement");

    private final String label;

    NutritionCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
