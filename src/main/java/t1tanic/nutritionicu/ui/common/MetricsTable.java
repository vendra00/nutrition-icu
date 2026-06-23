package t1tanic.nutritionicu.ui.common;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;

/**
 * A compact, reusable two-column Metric/Value table used across the views. The value cell renders as a
 * coloured BMI pill ({@link BmiBadge}) when a row carries a BMI, and as plain text otherwise. Callers
 * supply the metric-column header and set their own width.
 */
public final class MetricsTable {

    private MetricsTable() {
    }

    /**
     * One row: a label, its display value, an optional BMI (non-null → coloured pill), an optional
     * {@code tooltip} shown on hover over the value, and an optional Lumo {@code badgeTheme} that renders
     * the value as a coloured badge (e.g. {@code "error"}, {@code "success"}).
     */
    public record Row(String metric, String value, Double bmi, String tooltip, String badgeTheme) {

        public Row(String metric, String value) {
            this(metric, value, null, null, null);
        }

        public Row(String metric, String value, Double bmi) {
            this(metric, value, bmi, null, null);
        }

        public Row(String metric, String value, Double bmi, String tooltip) {
            this(metric, value, bmi, tooltip, null);
        }

        /** A row whose value renders as a Lumo badge (e.g. {@code "error"}, {@code "success"}). */
        public static Row badge(String metric, String value, String lumoBadgeTheme) {
            return new Row(metric, value, null, null, lumoBadgeTheme);
        }

        Span valueComponent() {
            Span span = bmi == null ? new Span(value) : BmiBadge.of(bmi, value);
            if (badgeTheme != null) {
                span.getElement().getThemeList().add("badge " + badgeTheme);
            }
            if (tooltip != null && !tooltip.isBlank()) {
                span.getElement().setAttribute("title", tooltip);
                span.getStyle().set("cursor", "help");
            }
            return span;
        }
    }

    /**
     * A configured grid (auto-height, row-striped, compact) with the given metric-column header and a
     * "Value" column. The value column grows to fill; the metric column sizes to content.
     */
    public static Grid<Row> create(String metricHeader) {
        Grid<Row> grid = new Grid<>();
        grid.addColumn(Row::metric).setHeader(metricHeader).setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(Row::valueComponent).setHeader(I18n.t("common.value"))
                .setAutoWidth(true).setFlexGrow(1);
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        return grid;
    }
}
