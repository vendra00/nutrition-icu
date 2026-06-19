package t1tanic.nutritionicu.ui.common;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
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

    private static final int W = 760;
    private static final int H = 280;
    private static final int ML = 55;  // left margin (y labels)
    private static final int MR = 20;
    private static final int MT = 20;
    private static final int MB = 35;  // bottom margin (x labels)

    public TrendChart(List<Point> points, Double refLow, Double refHigh, String unit) {
        getContent().setWidthFull();
        if (points.isEmpty()) {
            getContent().add(new Paragraph("No numeric data for this analyte."));
            return;
        }
        getContent().getElement().setProperty("innerHTML", svg(points, refLow, refHigh, unit));
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
        return LocalDate.ofInstant(instant, ZoneId.systemDefault()).toString();
    }

    private static String f(double v) {
        return String.format(Locale.US, "%.1f", v);
    }
}
