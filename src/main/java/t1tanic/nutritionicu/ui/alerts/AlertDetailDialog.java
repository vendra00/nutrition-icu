package t1tanic.nutritionicu.ui.alerts;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;
import t1tanic.nutritionicu.dto.AlertSummary;
import t1tanic.nutritionicu.service.patient.PatientOverviewService;
import t1tanic.nutritionicu.ui.common.UiFormat;
import t1tanic.nutritionicu.ui.patients.PatientOverviewDialog;

/**
 * Read-only full view of a single alert, opened from the Severity link in the Alerts grid. Shows the alert
 * fields as a Field / Value table (the patient NHC links to the same overview used on the Patients tab), a
 * refeeding-syndrome warning when flagged, and the abnormal lab readings as an Analyte / Value / Flag grid.
 */
public class AlertDetailDialog extends Dialog {

    private record Field(String field, Component value) {
    }

    private final transient PatientOverviewService overviewService;

    public AlertDetailDialog(AlertSummary alert, PatientOverviewService overviewService) {
        this.overviewService = overviewService;
        setHeaderTitle(getTranslation("alerts.detail.title"));
        setWidth("560px");

        VerticalLayout content = new VerticalLayout(headerTable(alert));
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("gap", "var(--lumo-space-m)");

        if (alert.refeedingRisk()) {
            Span warning = new Span(getTranslation("alerts.detail.refeeding"));
            warning.getElement().getThemeList().add("badge error");
            content.add(warning);
        }
        if (!alert.results().isEmpty()) {
            Span heading = new Span(getTranslation("alerts.detail.results"));
            heading.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            content.add(heading, resultsGrid(alert.results()));
        }

        add(content);
        getFooter().add(new Button(getTranslation("common.close"), e -> close()));
    }

    private Grid<Field> headerTable(AlertSummary alert) {
        Grid<Field> grid = new Grid<>();
        grid.addColumn(Field::field).setHeader(getTranslation("overview.field")).setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(Field::value).setHeader(getTranslation("common.value")).setFlexGrow(1);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);
        grid.setAllRowsVisible(true);
        grid.setItems(List.of(
                new Field(getTranslation("alerts.col.severity"), text(getTranslation("alertSeverity." + alert.severity()))),
                new Field(getTranslation("alerts.col.status"), text(getTranslation("alertStatus." + alert.status()))),
                new Field(getTranslation("alerts.col.patient"), patientValue(alert)),
                new Field(getTranslation("alerts.col.sectors"), text(sectorsText(alert.sectors()))),
                new Field(getTranslation("alerts.col.raised"), text(UiFormat.instant(alert.createdAt())))));
        return grid;
    }

    /** The NHC as a link that opens the patient overview, or plain text when no patient id is available. */
    private Component patientValue(AlertSummary alert) {
        if (alert.patientId() == null) {
            return text(alert.patientMrn());
        }
        Button link = new Button(alert.patientMrn(),
                e -> new PatientOverviewDialog(alert.patientId(), overviewService).open());
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        return link;
    }

    private Grid<AlertSummary.AlertResult> resultsGrid(List<AlertSummary.AlertResult> results) {
        Grid<AlertSummary.AlertResult> grid = new Grid<>();
        grid.addColumn(AlertSummary.AlertResult::analyte)
                .setHeader(getTranslation("analytics.col.analyte")).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(AlertSummary.AlertResult::value)
                .setHeader(getTranslation("analytics.col.value")).setAutoWidth(true);
        grid.addColumn(AlertSummary.AlertResult::flag)
                .setHeader(getTranslation("analytics.col.flag")).setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setAllRowsVisible(true);
        grid.setItems(results);
        return grid;
    }

    private static Span text(String value) {
        Span span = new Span(value);
        span.getStyle().set("white-space", "normal");
        return span;
    }

    /** Translates the comma-separated sector codes stored on the summary. */
    private String sectorsText(String joined) {
        if (joined == null || joined.isBlank()) {
            return "";
        }
        String[] parts = joined.split(",\\s*");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(getTranslation("sector." + parts[i].strip()));
        }
        return sb.toString();
    }
}
