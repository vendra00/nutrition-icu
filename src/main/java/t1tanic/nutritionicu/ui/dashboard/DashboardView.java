package t1tanic.nutritionicu.ui.dashboard;
import t1tanic.nutritionicu.ui.common.BarList;
import t1tanic.nutritionicu.ui.common.Donut;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import t1tanic.nutritionicu.dto.AlertFilter;
import t1tanic.nutritionicu.dto.AlertSummary;
import t1tanic.nutritionicu.dto.DashboardStats;
import t1tanic.nutritionicu.model.enums.AlertSeverity;
import t1tanic.nutritionicu.service.alert.AlertService;
import t1tanic.nutritionicu.service.dashboard.DashboardService;
import t1tanic.nutritionicu.ui.common.I18n;

/** Landing overview: headline metrics, cohort breakdown charts and the most recent alerts. */
@Route(value = "", layout = MainLayout.class)
@PermitAll
public class DashboardView extends VerticalLayout implements HasDynamicTitle {

    @Override
    public String getPageTitle() {
        return getTranslation("dashboard.title") + " · " + getTranslation("app.title");
    }

    private static final String RED = "#C62828";
    private static final String GREEN = "#2E7D32";
    private static final String ORANGE = "#E65100";
    private static final String AMBER = "#8A6D00";
    private static final String GREY = "#9E9E9E";

    private final transient AlertService alertService;
    private AlertFilter alertFilter = AlertFilter.empty();

    public DashboardView(DashboardService dashboardService, AlertService alertService) {
        this.alertService = alertService;
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        DashboardStats s = dashboardService.stats();
        add(new H2(getTranslation("dashboard.title")));

        add(row(
                statCard(getTranslation("dashboard.monitored"), String.valueOf(s.monitoredPatients()),
                        "var(--lumo-primary-text-color)"),
                statCard(getTranslation("dashboard.activealerts"), String.valueOf(s.activeAlerts()),
                        s.activeAlerts() > 0 ? RED : GREEN),
                statCard(getTranslation("dashboard.highrisk"), String.valueOf(s.highRisk()),
                        s.highRisk() > 0 ? RED : GREEN),
                statCard(getTranslation("dashboard.avgdelivery"),
                        s.avgPercentDelivered() == null ? "—" : s.avgPercentDelivered() + "%",
                        deliveryColor(s.avgPercentDelivered()))));

        add(row(
                chartCard(getTranslation("dashboard.nutricrisk"), new Donut(List.of(
                        new Donut.Slice(getTranslation("risk.high"), s.highRisk(), RED),
                        new Donut.Slice(getTranslation("risk.low"), s.lowRisk(), GREEN),
                        new Donut.Slice(getTranslation("risk.notassessed"), s.notAssessed(), GREY)),
                        String.valueOf(s.monitoredPatients()))),
                chartCard(getTranslation("dashboard.bmidist"), new BarList(List.of(
                        new BarList.Bar(getTranslation("bmi.underweight"), s.underweight(), AMBER),
                        new BarList.Bar(getTranslation("bmi.normal"), s.normalWeight(), GREEN),
                        new BarList.Bar(getTranslation("bmi.overweight"), s.overweight(), ORANGE),
                        new BarList.Bar(getTranslation("bmi.obese"), s.obese(), RED)))),
                chartCard(getTranslation("dashboard.alertsbyseverity"), new Donut(List.of(
                        new Donut.Slice(getTranslation("alertSeverity.CRITICAL"), s.criticalAlerts(), RED),
                        new Donut.Slice(getTranslation("alertSeverity.WARNING"), s.warningAlerts(), ORANGE)),
                        String.valueOf(s.activeAlerts())))));

        add(recentAlertsCard());
    }

    private static Div row(Component... cards) {
        Div row = new Div(cards);
        row.getStyle().set("display", "flex").set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-m)").set("width", "100%");
        return row;
    }

    private static Component statCard(String label, String value, String accent) {
        Span number = new Span(value);
        number.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD);
        number.getStyle().set("color", accent);
        Span caption = new Span(label);
        caption.addClassNames(LumoUtility.TextColor.SECONDARY);
        VerticalLayout card = new VerticalLayout(number, caption);
        card.setSpacing(false);
        card.setPadding(true);
        card.getStyle().set("flex", "1").set("min-width", "180px");
        decorate(card);
        return card;
    }

    private static Component chartCard(String title, Component body) {
        H3 heading = new H3(title);
        heading.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.NONE);
        VerticalLayout card = new VerticalLayout(heading, body);
        card.setSpacing(true);
        card.setPadding(true);
        card.getStyle().set("flex", "1").set("min-width", "300px");
        decorate(card);
        return card;
    }

    private Component recentAlertsCard() {
        Grid<AlertSummary> grid = new Grid<>(AlertSummary.class, false);
        Grid.Column<AlertSummary> severityCol = grid.addComponentColumn(DashboardView::severityBadge)
                .setHeader(getTranslation("alerts.col.severity")).setAutoWidth(true);
        Grid.Column<AlertSummary> patientCol = grid.addColumn(AlertSummary::patientMrn)
                .setHeader(getTranslation("alerts.col.patient")).setAutoWidth(true);
        grid.addColumn(a -> sectorsText(a.sectors())).setHeader(getTranslation("alerts.col.sectors")).setAutoWidth(true);
        Grid.Column<AlertSummary> detailsCol = grid.addColumn(AlertSummary::message)
                .setHeader(getTranslation("alerts.col.details")).setFlexGrow(3);
        grid.setHeight("420px");

        // Lazy loading: the grid fetches pages from the backend as it scrolls, not all at once.
        CallbackDataProvider<AlertSummary, Void> dataProvider = DataProvider.fromCallbacks(
                query -> alertService.search(alertFilter, query.getOffset(), query.getLimit()).stream(),
                query -> (int) alertService.count(alertFilter));
        grid.setItems(dataProvider);

        // Header filter row: severity / patient / details. Changing any re-queries the backend.
        HeaderRow filterRow = grid.appendHeaderRow();
        ComboBox<AlertSeverity> severity = new ComboBox<>();
        severity.setItems(AlertSeverity.values());
        severity.setItemLabelGenerator(v -> getTranslation("alertSeverity." + v.name()));
        severity.setPlaceholder(getTranslation("filter.all"));
        severity.setClearButtonVisible(true);
        severity.setWidthFull();
        TextField patient = filterField(getTranslation("filter.nhc"));
        TextField details = filterField(getTranslation("filter.search"));

        Runnable apply = () -> {
            alertFilter = new AlertFilter(severity.getValue(), null,
                    blankToNull(patient.getValue()), blankToNull(details.getValue()));
            dataProvider.refreshAll();
        };
        severity.addValueChangeListener(e -> apply.run());
        patient.addValueChangeListener(e -> apply.run());
        details.addValueChangeListener(e -> apply.run());
        filterRow.getCell(severityCol).setComponent(severity);
        filterRow.getCell(patientCol).setComponent(patient);
        filterRow.getCell(detailsCol).setComponent(details);

        H3 heading = new H3(getTranslation("dashboard.recentalerts"));
        heading.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.NONE);
        VerticalLayout card = new VerticalLayout(heading, grid);
        card.setPadding(true);
        card.setWidthFull();
        decorate(card);
        return card;
    }

    private static TextField filterField(String placeholder) {
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

    private static Span severityBadge(AlertSummary alert) {
        Span badge = new Span(I18n.t("alertSeverity." + alert.severity()));
        badge.getElement().getThemeList().add("badge " + ("CRITICAL".equals(alert.severity()) ? "error" : "warning"));
        return badge;
    }

    private static void decorate(VerticalLayout card) {
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("background-color", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)");
    }

    private static String deliveryColor(Integer pct) {
        if (pct == null) {
            return "var(--lumo-secondary-text-color)";
        }
        if (pct >= 90) {
            return GREEN;
        }
        return pct >= 70 ? ORANGE : RED;
    }
}
