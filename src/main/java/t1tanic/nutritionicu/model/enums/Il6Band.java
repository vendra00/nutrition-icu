package t1tanic.nutritionicu.model.enums;

/** NUTRIC IL-6 band (optional; when chosen, the 10-point variant is used). */
public enum Il6Band implements NutricBand {
    LT_400("< 400", 0),
    GE_400("≥ 400", 1);

    private final String label;
    private final int points;

    Il6Band(String label, int points) {
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
