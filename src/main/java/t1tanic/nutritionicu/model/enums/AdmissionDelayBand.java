package t1tanic.nutritionicu.model.enums;

/** NUTRIC "days from hospital to ICU admission" band. */
public enum AdmissionDelayBand implements NutricBand {
    LT_1("< 1 day", 0),
    GE_1("≥ 1 day", 1);

    private final String label;
    private final int points;

    AdmissionDelayBand(String label, int points) {
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
