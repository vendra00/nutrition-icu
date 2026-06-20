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
     * One row: a label, its display value, an optional BMI (non-null → coloured pill), and an optional
     * {@code tooltip} shown on hover over the value (e.g. the date behind a "latest" reading).
     */
    public record Row(String metric, String value, Double bmi, String tooltip) {

        public Row(String metric, String value) {
            this(metric, value, null, null);
        }

        public Row(String metric, String value, Double bmi) {
            this(metric, value, bmi, null);
        }

        Span valueComponent() {
            Span span = bmi == null ? new Span(value) : BmiBadge.of(bmi, value);
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
        grid.addComponentColumn(Row::valueComponent).setHeader("Value").setAutoWidth(true).setFlexGrow(1);
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        return grid;
    }
}
