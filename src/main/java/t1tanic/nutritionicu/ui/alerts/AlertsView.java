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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import t1tanic.nutritionicu.dto.AlertFilter;
import t1tanic.nutritionicu.dto.AlertSummary;
import t1tanic.nutritionicu.model.enums.AlertSeverity;
import t1tanic.nutritionicu.model.enums.AlertStatus;
import t1tanic.nutritionicu.service.alert.AlertService;

/** All alerts raised for monitored patients, newest first, with header filters. */
@Route(value = "alerts", layout = MainLayout.class)
@PageTitle("Alerts · ICU Nutrition")
@PermitAll
public class AlertsView extends VerticalLayout {

    private final transient AlertService alertService;
    private AlertFilter filter = AlertFilter.empty();

    public AlertsView(AlertService alertService) {
        this.alertService = alertService;
        setSizeFull();
        setPadding(true);
        add(new H2("Alerts"));

        Grid<AlertSummary> grid = new Grid<>(AlertSummary.class, false);
        Grid.Column<AlertSummary> severityCol = grid.addColumn(AlertSummary::severity)
                .setHeader("Severity").setAutoWidth(true);
        Grid.Column<AlertSummary> statusCol = grid.addColumn(AlertSummary::status)
                .setHeader("Status").setAutoWidth(true);
        Grid.Column<AlertSummary> patientCol = grid.addColumn(AlertSummary::patientMrn)
                .setHeader("Patient (NHC)").setAutoWidth(true);
        grid.addColumn(AlertSummary::sectors).setHeader("Sectors").setAutoWidth(true);
        Grid.Column<AlertSummary> detailsCol = grid.addColumn(AlertSummary::message)
                .setHeader("Details").setFlexGrow(3);
        grid.addColumn(AlertSummary::createdAt).setHeader("Raised").setAutoWidth(true);

        // Lazy loading: the grid fetches pages from the backend as it scrolls, not all at once.
        CallbackDataProvider<AlertSummary, Void> dataProvider = DataProvider.fromCallbacks(
                query -> alertService.search(filter, query.getOffset(), query.getLimit()).stream(),
                query -> (int) alertService.count(filter));
        grid.setItems(dataProvider);
        grid.setSizeFull();

        ComboBox<AlertSeverity> severity = enumFilter(AlertSeverity.values());
        ComboBox<AlertStatus> status = enumFilter(AlertStatus.values());
        TextField patient = textFilter("NHC…");
        TextField details = textFilter("Search…");

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

    private static <T extends Enum<T>> ComboBox<T> enumFilter(T[] items) {
        ComboBox<T> box = new ComboBox<>();
        box.setItems(items);
        box.setItemLabelGenerator(Enum::name);
        box.setPlaceholder("All");
        box.setClearButtonVisible(true);
        box.setWidthFull();
        return box;
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
