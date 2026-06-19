package t1tanic.nutritionicu.model.enums;

/**
 * BMI band that decides which body weight the Harris-Benedict equation is fed, per the
 * rccc.eu calculator: actual weight when normal, otherwise an obesity-adjusted weight.
 */
public enum WeightClass {
    NORMAL("Normal weight", null),
    OBESE("Obesity", 0.25),
    MORBIDLY_OBESE("Morbid/extreme obesity", 0.5);

    private final String label;
    private final Double adjustmentConstant;

    WeightClass(String label, Double adjustmentConstant) {
        this.label = label;
        this.adjustmentConstant = adjustmentConstant;
    }

    public String label() {
        return label;
    }

    /** Fraction of (actual − ideal) added to ideal weight; {@code null} when actual weight is used as-is. */
    public Double adjustmentConstant() {
        return adjustmentConstant;
    }

    /** The band for a (rounded) BMI: &lt;30 normal, 30–40 obese, &gt;40 morbidly obese. */
    public static WeightClass forBmi(double bmi) {
        if (bmi < 30) {
            return NORMAL;
        }
        return bmi <= 40 ? OBESE : MORBIDLY_OBESE;
    }
}
