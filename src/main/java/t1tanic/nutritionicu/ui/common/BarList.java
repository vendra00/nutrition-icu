package t1tanic.nutritionicu.ui.common;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.util.List;
import java.util.Locale;

/** A dependency-free horizontal bar chart (CSS bars), bars scaled to the largest value. */
public class BarList extends Composite<Div> {

    public record Bar(String label, double value, String color) {
    }

    public BarList(List<Bar> bars) {
        Div root = getContent();
        root.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)").set("width", "100%");

        double max = bars.stream().mapToDouble(Bar::value).max().orElse(0);
        double scale = max <= 0 ? 1 : max;
        for (Bar bar : bars) {
            Span label = new Span(bar.label());
            label.getStyle().set("width", "10em").set("flex-shrink", "0")
                    .set("font-size", "var(--lumo-font-size-s)");

            Div fill = new Div();
            fill.getStyle().set("width", String.format(Locale.US, "%.1f%%", bar.value() / scale * 100))
                    .set("min-width", bar.value() > 0 ? "3px" : "0")
                    .set("height", "1.4em").set("background", bar.color())
                    .set("border-radius", "var(--lumo-border-radius-s)");
            Div track = new Div(fill);
            track.getStyle().set("flex", "1").set("background", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "var(--lumo-border-radius-s)").set("overflow", "hidden");

            Span value = new Span(String.valueOf(Math.round(bar.value())));
            value.getStyle().set("width", "2.5em").set("text-align", "right")
                    .set("font-weight", "500").set("font-size", "var(--lumo-font-size-s)");

            Div row = new Div(label, track, value);
            row.getStyle().set("display", "flex").set("align-items", "center")
                    .set("gap", "var(--lumo-space-s)").set("width", "100%");
            root.add(row);
        }
    }
}
