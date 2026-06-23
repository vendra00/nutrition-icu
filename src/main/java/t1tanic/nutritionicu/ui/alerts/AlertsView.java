package t1tanic.nutritionicu.ui.alerts;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import t1tanic.nutritionicu.service.patient.PatientOverviewService;
import t1tanic.nutritionicu.ui.common.I18n;
import t1tanic.nutritionicu.ui.common.UiFormat;

/** All alerts raised for monitored patients, newest first, with header filters. */
@Route(value = "alerts", layout = MainLayout.class)
@PermitAll
public class AlertsView extends VerticalLayout implements HasDynamicTitle {

    @Override
    public String getPageTitle() {
        return getTranslation("alerts.title") + " · " + getTranslation("app.title");
    }

    private final transient AlertService alertService;
    private final transient PatientOverviewService overviewService;
    private AlertFilter filter = AlertFilter.empty();

    public AlertsView(AlertService alertService, PatientOverviewService overviewService) {
        this.alertService = alertService;
        this.overviewService = overviewService;
        setSizeFull();
        setPadding(true);
        add(new H2(getTranslation("alerts.title")));

        Grid<AlertSummary> grid = new Grid<>(AlertSummary.class, false);
        Grid.Column<AlertSummary> severityCol = grid.addComponentColumn(this::severityLink)
                .setHeader(getTranslation("alerts.col.severity")).setAutoWidth(true);
        Grid.Column<AlertSummary> statusCol = grid.addColumn(s -> getTranslation("alertStatus." + s.status()))
                .setHeader(getTranslation("alerts.col.status")).setAutoWidth(true);
        Grid.Column<AlertSummary> patientCol = grid.addColumn(AlertSummary::patientMrn)
                .setHeader(getTranslation("alerts.col.patient")).setAutoWidth(true);
        grid.addColumn(s -> UiFormat.instant(s.createdAt()))
                .setHeader(getTranslation("alerts.col.raised")).setAutoWidth(true).setFlexGrow(1);

        // Lazy loading: the grid fetches pages from the backend as it scrolls, not all at once.
        CallbackDataProvider<AlertSummary, Void> dataProvider = DataProvider.fromCallbacks(
                query -> alertService.search(filter, query.getOffset(), query.getLimit()).stream(),
                query -> (int) alertService.count(filter));
        grid.setItems(dataProvider);
        grid.setSizeFull();

        ComboBox<AlertSeverity> severity = enumFilter(AlertSeverity.values(), "alertSeverity");
        ComboBox<AlertStatus> status = enumFilter(AlertStatus.values(), "alertStatus");
        TextField patient = textFilter(getTranslation("filter.nhc"));

        Runnable apply = () -> {
            filter = new AlertFilter(severity.getValue(), status.getValue(),
                    blankToNull(patient.getValue()), null);
            dataProvider.refreshAll();
        };
        severity.addValueChangeListener(e -> apply.run());
        status.addValueChangeListener(e -> apply.run());
        patient.addValueChangeListener(e -> apply.run());

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(severityCol).setComponent(severity);
        filterRow.getCell(statusCol).setComponent(status);
        filterRow.getCell(patientCol).setComponent(patient);

        addAndExpand(grid);
    }

    /** Severity rendered as a link that opens the full alert detail. */
    private Button severityLink(AlertSummary alert) {
        Button link = new Button(getTranslation("alertSeverity." + alert.severity()),
                e -> new AlertDetailDialog(alert, overviewService).open());
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        return link;
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
