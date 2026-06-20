package t1tanic.nutritionicu.ui.common;

import com.vaadin.flow.component.html.Span;

/**
 * Renders a BMI value as a traffic-light coloured pill: green for normal weight, yellow for a mild
 * deviation (under/overweight), orange for obese, red for the severe tails (severe underweight or
 * severely obese). WHO cut-offs. The category is also exposed as a hover tooltip.
 */
public final class BmiBadge {

    private BmiBadge() {
    }

    private enum Band {
        UNDERWEIGHT_SEVERE("Severe underweight", "#FCE4E4", "#C62828"),
        UNDERWEIGHT("Underweight", "#FFF6DB", "#8A6D00"),
        NORMAL("Normal", "#E6F4EA", "#2E7D32"),
        OVERWEIGHT("Overweight", "#FFF6DB", "#8A6D00"),
        OBESE("Obese", "#FFEBD6", "#E65100"),
        OBESE_SEVERE("Severely obese", "#FCE4E4", "#C62828");

        final String label;
        final String background;
        final String foreground;

        Band(String label, String background, String foreground) {
            this.label = label;
            this.background = background;
            this.foreground = foreground;
        }
    }

    private static Band band(double bmi) {
        if (bmi < 16.0) {
            return Band.UNDERWEIGHT_SEVERE;
        }
        if (bmi < 18.5) {
            return Band.UNDERWEIGHT;
        }
        if (bmi < 25.0) {
            return Band.NORMAL;
        }
        if (bmi < 30.0) {
            return Band.OVERWEIGHT;
        }
        if (bmi < 35.0) {
            return Band.OBESE;
        }
        return Band.OBESE_SEVERE;
    }

    /**
     * A coloured BMI pill, or a plain {@link UiFormat#EMPTY} placeholder when {@code bmi} is null.
     * Formats the value itself, so callers need not.
     */
    public static Span ofNullable(Double bmi) {
        String text = UiFormat.number(bmi);
        return bmi == null ? new Span(text) : of(bmi, text);
    }

    /** A coloured pill showing {@code text} (the formatted BMI), banded by {@code bmi}. */
    public static Span of(double bmi, String text) {
        Band band = band(bmi);
        Span pill = new Span(text);
        pill.getElement().setAttribute("title", band.label);
        pill.getStyle()
                .set("background-color", band.background)
                .set("color", band.foreground)
                .set("padding", "0.1em 0.6em")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("font-weight", "500")
                .set("white-space", "nowrap");
        return pill;
    }
}
