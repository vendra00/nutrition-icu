package t1tanic.nutritionicu.ui.common;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.util.List;
import java.util.Locale;

/**
 * A dependency-free donut chart (inline SVG with a coloured legend), in the same spirit as
 * {@link TrendChart}. Slices are drawn as stroked-circle arcs; a centre label shows the total.
 */
public class Donut extends Composite<Div> {

    public record Slice(String label, double value, String color) {
    }

    private static final int SIZE = 168;
    private static final double R = 60;
    private static final double STROKE = 22;

    public Donut(List<Slice> slices, String centerLabel) {
        Div root = getContent();
        root.getStyle().set("display", "flex").set("align-items", "center")
                .set("gap", "var(--lumo-space-l)").set("flex-wrap", "wrap");

        double total = slices.stream().mapToDouble(Slice::value).sum();
        Div svgHolder = new Div();
        svgHolder.getElement().setProperty("innerHTML", svg(slices, total, centerLabel));
        root.add(svgHolder, legend(slices));
    }

    private static String svg(List<Slice> slices, double total, String centerLabel) {
        double c = 2 * Math.PI * R;
        double cx = SIZE / 2.0;
        double cy = SIZE / 2.0;
        StringBuilder sb = new StringBuilder();
        sb.append("<svg width=\"").append(SIZE).append("\" height=\"").append(SIZE)
                .append("\" viewBox=\"0 0 ").append(SIZE).append(" ").append(SIZE)
                .append("\" style=\"font-family:sans-serif\">");
        sb.append("<circle cx=\"").append(f(cx)).append("\" cy=\"").append(f(cy)).append("\" r=\"").append(f(R))
                .append("\" fill=\"none\" stroke=\"var(--lumo-contrast-10pct)\" stroke-width=\"").append(f(STROKE)).append("\"/>");
        if (total > 0) {
            double cumulative = 0;
            for (Slice s : slices) {
                if (s.value() <= 0) {
                    continue;
                }
                double fraction = s.value() / total;
                double length = fraction * c;
                sb.append("<circle cx=\"").append(f(cx)).append("\" cy=\"").append(f(cy)).append("\" r=\"").append(f(R))
                        .append("\" fill=\"none\" stroke=\"").append(s.color())
                        .append("\" stroke-width=\"").append(f(STROKE))
                        .append("\" stroke-dasharray=\"").append(f(length)).append(" ").append(f(c - length))
                        .append("\" stroke-dashoffset=\"").append(f(-cumulative * c))
                        .append("\" transform=\"rotate(-90 ").append(f(cx)).append(" ").append(f(cy)).append(")\"/>");
                cumulative += fraction;
            }
        }
        String big = centerLabel != null ? centerLabel : i(total);
        sb.append("<text x=\"").append(f(cx)).append("\" y=\"").append(f(cy + 7))
                .append("\" text-anchor=\"middle\" font-size=\"26\" font-weight=\"700\" fill=\"var(--lumo-body-text-color)\">")
                .append(big).append("</text>");
        sb.append("</svg>");
        return sb.toString();
    }

    private static Div legend(List<Slice> slices) {
        Div box = new Div();
        box.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "var(--lumo-space-xs)");
        for (Slice s : slices) {
            Span dot = new Span();
            dot.getStyle().set("display", "inline-block").set("width", "10px").set("height", "10px")
                    .set("border-radius", "50%").set("background", s.color()).set("margin-right", "6px");
            Span label = new Span(s.label() + " · " + i(s.value()));
            label.getStyle().set("font-size", "var(--lumo-font-size-s)");
            Div row = new Div(dot, label);
            row.getStyle().set("display", "inline-flex").set("align-items", "center");
            box.add(row);
        }
        return box;
    }

    private static String f(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private static String i(double v) {
        return String.valueOf(Math.round(v));
    }
}
