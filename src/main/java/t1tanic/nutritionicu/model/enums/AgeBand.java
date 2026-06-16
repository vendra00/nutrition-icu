package t1tanic.nutritionicu.model.enums;

/** NUTRIC age band (derived from the patient's date of birth). */
public enum AgeBand implements NutricBand {
    LT_50("< 50", 0),
    B50_74("50–74", 1),
    GE_75("≥ 75", 2);

    private final String label;
    private final int points;

    AgeBand(String label, int points) {
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

    public static AgeBand fromAge(int ageYears) {
        if (ageYears >= 75) {
            return GE_75;
        }
        return ageYears >= 50 ? B50_74 : LT_50;
    }
}
