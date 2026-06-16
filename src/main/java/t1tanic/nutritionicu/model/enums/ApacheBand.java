package t1tanic.nutritionicu.model.enums;

/** NUTRIC APACHE II band. */
public enum ApacheBand implements NutricBand {
    LT_15("< 15", 0),
    B15_19("15–19", 1),
    B20_27("20–27", 2),
    GE_28("≥ 28", 3);

    private final String label;
    private final int points;

    ApacheBand(String label, int points) {
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
