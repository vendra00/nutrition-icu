package t1tanic.nutritionicu.model.enums;

/** NUTRIC comorbidity count band. */
public enum ComorbidityBand implements NutricBand {
    LE_1("0–1", 0),
    GE_2("≥ 2", 1);

    private final String label;
    private final int points;

    ComorbidityBand(String label, int points) {
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
