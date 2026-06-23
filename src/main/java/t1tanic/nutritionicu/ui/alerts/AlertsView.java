package t1tanic.nutritionicu.ui.alerts;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import t1tanic.nutritionicu.dto.AlertFilter;
import t1tanic.nutritionicu.dto.AlertSummary;
import t1tanic.nutritionicu.model.enums.AlertSeverity;
import t1tanic.nutritionicu.model.enums.AlertStatus;
import t1tanic.nutritionicu.service.alert.AlertService;
import t1tanic.nutritionicu.ui.common.I18n;

/** All alerts raised for monitored patients, newest first, with header filters. */
@Route(value = "alerts", layout = MainLayout.class)
@PermitAll
public class AlertsView extends VerticalLayout implements HasDynamicTitle {

    @Override
    public String getPageTitle() {
        return getTranslation("alerts.title") + " · " + getTranslation("app.title");
    }

    private final transient AlertService alertService;
    private AlertFilter filter = AlertFilter.empty();

    public AlertsView(AlertService alertService) {
        this.alertService = alertService;
        setSizeFull();
        setPadding(true);
        add(new H2(getTranslation("alerts.title")));

        Grid<AlertSummary> grid = new Grid<>(AlertSummary.class, false);
        Grid.Column<AlertSummary> severityCol = grid.addColumn(s -> getTranslation("alertSeverity." + s.severity()))
                .setHeader(getTranslation("alerts.col.severity")).setAutoWidth(true);
        Grid.Column<AlertSummary> statusCol = grid.addColumn(s -> getTranslation("alertStatus." + s.status()))
                .setHeader(getTranslation("alerts.col.status")).setAutoWidth(true);
        Grid.Column<AlertSummary> patientCol = grid.addColumn(AlertSummary::patientMrn)
                .setHeader(getTranslation("alerts.col.patient")).setAutoWidth(true);
        grid.addColumn(s -> sectorsText(s.sectors())).setHeader(getTranslation("alerts.col.sectors")).setAutoWidth(true);
        Grid.Column<AlertSummary> detailsCol = grid.addColumn(AlertSummary::message)
                .setHeader(getTranslation("alerts.col.details")).setFlexGrow(3);
        grid.addColumn(AlertSummary::createdAt).setHeader(getTranslation("alerts.col.raised")).setAutoWidth(true);

        // Lazy loading: the grid fetches pages from the backend as it scrolls, not all at once.
        CallbackDataProvider<AlertSummary, Void> dataProvider = DataProvider.fromCallbacks(
                query -> alertService.search(filter, query.getOffset(), query.getLimit()).stream(),
                query -> (int) alertService.count(filter));
        grid.setItems(dataProvider);
        grid.setSizeFull();

        ComboBox<AlertSeverity> severity = enumFilter(AlertSeverity.values(), "alertSeverity");
        ComboBox<AlertStatus> status = enumFilter(AlertStatus.values(), "alertStatus");
        TextField patient = textFilter(getTranslation("filter.nhc"));
        TextField details = textFilter(getTranslation("filter.search"));

        Runnable apply = () -> {
            filter = new AlertFilter(severity.getValue(), status.getValue(),
                    blankToNull(patient.getValue()), blankToNull(details.getValue()));
            dataProvider.refreshAll();
        };
        severity.addValueChangeListener(e -> apply.run());
        status.addValueChangeListener(e -> apply.run());
        patient.addValueChangeListener(e -> apply.run());
        details.addValueChangeListener(e -> apply.run());

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(severityCol).setComponent(severity);
        filterRow.getCell(statusCol).setComponent(status);
        filterRow.getCell(patientCol).setComponent(patient);
        filterRow.getCell(detailsCol).setComponent(details);

        addAndExpand(grid);
    }

    private static <T extends Enum<T>> ComboBox<T> enumFilter(T[] items, String keyPrefix) {
        ComboBox<T> box = new ComboBox<>();
        box.setItems(items);
        box.setItemLabelGenerator(value -> I18n.t(keyPrefix + "." + value.name()));
        box.setPlaceholder(I18n.t("filter.all"));
        box.setClearButtonVisible(true);
        box.setWidthFull();
        return box;
    }

    /** Translates a comma-separated list of sector codes (as stored on the summary). */
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

    private static TextField textFilter(String placeholder) {
        TextField field = new TextField();
        field.setPlaceholder(placeholder);
        field.setClearButtonVisible(true);
        field.setValueChangeMode(ValueChangeMode.LAZY);
        field.setWidthFull();
        return field;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
