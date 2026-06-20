package t1tanic.nutritionicu.ui.common;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * A dependency-free line chart rendered as inline SVG (pure Java, works on any
 * Vaadin version). Plots a value-over-time series with the optional reference
 * range drawn as a shaded band.
 */
public class TrendChart extends Composite<Div> {

    public record Point(Instant time, double value) {
    }

    /** A named line for the multi-series overlay; colour is assigned by the chart. */
    public record Series(String label, List<Point> points) {
    }

    private static final int W = 760;
    private static final int H = 280;
    private static final int ML = 55;  // left margin (y labels)
    private static final int MR = 20;
    private static final int MT = 20;
    private static final int MB = 35;  // bottom margin (x labels)

    /** Distinct line colours, reused cyclically when there are more series than colours. */
    private static final List<String> PALETTE = List.of(
            "#1565c0", "#c62828", "#2e7d32", "#e65100", "#6a1b9a", "#00838f", "#ad1457");

    public TrendChart(List<Point> points, Double refLow, Double refHigh, String unit) {
        getContent().setWidthFull();
        if (points.isEmpty()) {
            getContent().add(new Paragraph("No numeric data for this analyte."));
            return;
        }
        getContent().getElement().setProperty("innerHTML", svg(points, refLow, refHigh, unit));
    }

    /**
     * Overlays several series on one timeline. Each line is normalised to its own min–max (so analytes
     * with different units/scales are comparable in shape/timing), drawn in its own colour with a legend.
     * Absolute values and reference bands are not shown in this mode — use the single-series constructor
     * for those.
     */
    public TrendChart(List<Series> series) {
        renderSeries(series, true, null, "each line scaled to its own range");
    }

    /**
     * Overlays several series that share one {@code unit} on a single absolute axis — real values are
     * kept and directly comparable. Use this only when the series truly share a unit; otherwise prefer
     * the normalised {@link #TrendChart(List)} overlay.
     */
    public TrendChart(List<Series> series, String sharedUnit) {
        renderSeries(series, false, sharedUnit,
                sharedUnit == null ? "shared scale" : "shared scale · " + sharedUnit);
    }

    private void renderSeries(List<Series> series, boolean normalized, String unit, String note) {
        getContent().setWidthFull();
        List<Series> data = series.stream().filter(s -> !s.points().isEmpty()).toList();
        if (data.isEmpty()) {
            getContent().add(new Paragraph("No numeric data for the selected analytes."));
            return;
        }
        getContent().add(legend(data, note));
        Div svgHolder = new Div();
        svgHolder.setWidthFull();
        svgHolder.getElement().setProperty("innerHTML", seriesSvg(data, normalized, unit));
        getContent().add(svgHolder);
    }

    private String svg(List<Point> points, Double refLow, Double refHigh, String unit) {
        double plotW = W - ML - MR;
        double plotH = H - MT - MB;

        double vMin = points.stream().mapToDouble(Point::value).min().orElse(0);
        double vMax = points.stream().mapToDouble(Point::value).max().orElse(1);
        if (refLow != null) {
            vMin = Math.min(vMin, refLow);
        }
        if (refHigh != null) {
            vMax = Math.max(vMax, refHigh);
        }
        if (vMin == vMax) {
            vMin -= 1;
            vMax += 1;
        }
        double pad = (vMax - vMin) * 0.1;
        vMin -= pad;
        vMax += pad;

        long tMin = points.getFirst().time().toEpochMilli();
        long tMax = points.getLast().time().toEpochMilli();
        final double vLo = vMin;
        final double vHi = vMax;
        final long tLo = tMin;
        final long tSpan = Math.max(1, tMax - tMin);

        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(W)
                .append("\" height=\"").append(H).append("\" style=\"font-family:sans-serif\">");

        // Reference band
        if (refLow != null && refHigh != null) {
            double yHigh = y(refHigh, vLo, vHi, plotH);
            double yLow = y(refLow, vLo, vHi, plotH);
            sb.append("<rect x=\"").append(ML).append("\" y=\"").append(f(yHigh))
                    .append("\" width=\"").append(f(plotW)).append("\" height=\"").append(f(yLow - yHigh))
                    .append("\" fill=\"#2e7d32\" fill-opacity=\"0.10\"/>");
        }

        // Axes
        sb.append(line(ML, MT, ML, MT + plotH, "#bbb"));            // y axis
        sb.append(line(ML, MT + plotH, ML + plotW, MT + plotH, "#bbb")); // x axis

        // Polyline + points
        StringBuilder poly = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            double x = points.size() == 1
                    ? ML + plotW / 2
                    : ML + (double) (p.time().toEpochMilli() - tLo) / tSpan * plotW;
            double yv = y(p.value(), vLo, vHi, plotH);
            poly.append(f(x)).append(',').append(f(yv)).append(' ');
            sb.append("<circle cx=\"").append(f(x)).append("\" cy=\"").append(f(yv))
                    .append("\" r=\"3.5\" fill=\"#1565c0\"/>");
        }
        sb.append("<polyline points=\"").append(poly.toString().trim())
                .append("\" fill=\"none\" stroke=\"#1565c0\" stroke-width=\"2\"/>");

        // Y labels (min/max), X labels (first/last date)
        sb.append(text(4, MT + 4, f(vHi) + (unit != null ? " " + unit : ""), "#666", "start"));
        sb.append(text(4, MT + plotH, f(vLo), "#666", "start"));
        sb.append(text(ML, H - 8, dateLabel(points.getFirst().time()), "#666", "start"));
        sb.append(text(ML + plotW, H - 8, dateLabel(points.getLast().time()), "#666", "end"));

        sb.append("</svg>");
        return sb.toString();
    }

    private String seriesSvg(List<Series> data, boolean normalized, String unit) {
        double plotW = W - ML - MR;
        double plotH = H - MT - MB;

        long tMin = Long.MAX_VALUE;
        long tMax = Long.MIN_VALUE;
        double gMin = Double.POSITIVE_INFINITY;
        double gMax = Double.NEGATIVE_INFINITY;
        for (Series s : data) {
            for (Point p : s.points()) {
                long t = p.time().toEpochMilli();
                tMin = Math.min(tMin, t);
                tMax = Math.max(tMax, t);
                gMin = Math.min(gMin, p.value());
                gMax = Math.max(gMax, p.value());
            }
        }
        final long tLo = tMin;
        final long tHi = tMax;
        final long tSpan = Math.max(1, tMax - tMin);

        // Shared absolute scale (only used when !normalized).
        if (gMin == gMax) {
            gMin -= 1;
            gMax += 1;
        }
        double vPad = (gMax - gMin) * 0.1;
        final double vLo = gMin - vPad;
        final double vHi = gMax + vPad;

        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(W)
                .append("\" height=\"").append(H).append("\" style=\"font-family:sans-serif\">");

        sb.append(line(ML, MT, ML, MT + plotH, "#bbb"));                  // y axis
        sb.append(line(ML, MT + plotH, ML + plotW, MT + plotH, "#bbb"));  // x axis

        for (int i = 0; i < data.size(); i++) {
            Series s = data.get(i);
            String color = PALETTE.get(i % PALETTE.size());
            double sMin = s.points().stream().mapToDouble(Point::value).min().orElse(0);
            double sMax = s.points().stream().mapToDouble(Point::value).max().orElse(1);
            double range = sMax - sMin;

            StringBuilder poly = new StringBuilder();
            for (Point p : s.points()) {
                double x = s.points().size() == 1
                        ? ML + plotW / 2
                        : ML + (double) (p.time().toEpochMilli() - tLo) / tSpan * plotW;
                double yv = normalized
                        ? MT + (1 - (range == 0 ? 0.5 : (p.value() - sMin) / range)) * plotH
                        : y(p.value(), vLo, vHi, plotH);
                poly.append(f(x)).append(',').append(f(yv)).append(' ');
                sb.append("<circle cx=\"").append(f(x)).append("\" cy=\"").append(f(yv))
                        .append("\" r=\"3\" fill=\"").append(color).append("\"/>");
            }
            sb.append("<polyline points=\"").append(poly.toString().trim())
                    .append("\" fill=\"none\" stroke=\"").append(color).append("\" stroke-width=\"2\"/>");
        }

        // Y labels: relative hints when normalised, real values otherwise.
        if (normalized) {
            sb.append(text(4, MT + 4, "high", "#999", "start"));
            sb.append(text(4, MT + plotH, "low", "#999", "start"));
        } else {
            sb.append(text(4, MT + 4, f(vHi) + (unit != null ? " " + unit : ""), "#666", "start"));
            sb.append(text(4, MT + plotH, f(vLo), "#666", "start"));
        }
        sb.append(text(ML, H - 8, dateLabel(Instant.ofEpochMilli(tLo)), "#666", "start"));
        sb.append(text(ML + plotW, H - 8, dateLabel(Instant.ofEpochMilli(tHi)), "#666", "end"));

        sb.append("</svg>");
        return sb.toString();
    }

    private Div legend(List<Series> data, String note) {
        Div legend = new Div();
        legend.getStyle().set("display", "flex").set("flex-wrap", "wrap")
                .set("align-items", "center").set("gap", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-xs)");
        for (int i = 0; i < data.size(); i++) {
            Span dot = new Span();
            dot.getStyle().set("display", "inline-block").set("width", "10px").set("height", "10px")
                    .set("border-radius", "50%").set("background", PALETTE.get(i % PALETTE.size()))
                    .set("margin-right", "6px");
            Div item = new Div(dot, new Span(data.get(i).label()));
            item.getStyle().set("display", "inline-flex").set("align-items", "center")
                    .set("font-size", "var(--lumo-font-size-s)");
            legend.add(item);
        }
        Span caption = new Span(note);
        caption.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");
        legend.add(caption);
        return legend;
    }

    private static double y(double value, double vLo, double vHi, double plotH) {
        return MT + (vHi - value) / (vHi - vLo) * plotH;
    }

    private static String line(double x1, double y1, double x2, double y2, String color) {
        return "<line x1=\"" + f(x1) + "\" y1=\"" + f(y1) + "\" x2=\"" + f(x2) + "\" y2=\"" + f(y2)
                + "\" stroke=\"" + color + "\" stroke-width=\"1\"/>";
    }

    private static String text(double x, double y, String content, String color, String anchor) {
        return "<text x=\"" + f(x) + "\" y=\"" + f(y) + "\" font-size=\"11\" fill=\"" + color
                + "\" text-anchor=\"" + anchor + "\">" + content + "</text>";
    }

    private static String dateLabel(Instant instant) {
        return UiFormat.date(LocalDate.ofInstant(instant, ZoneId.systemDefault()));
    }

    private static String f(double v) {
        return String.format(Locale.US, "%.1f", v);
    }
}
