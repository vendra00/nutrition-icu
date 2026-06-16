package t1tanic.nutritionicu.model.enums;

/** NUTRIC SOFA band. */
public enum SofaBand implements NutricBand {
    LT_6("< 6", 0),
    B6_9("6–9", 1),
    GE_10("≥ 10", 2);

    private final String label;
    private final int points;

    SofaBand(String label, int points) {
        this.label = label;
        this.points = points;
    }

    @Override
    public int points() {
        return points;
    }

    @Override
    public String label() {
        return label;
    }
}
