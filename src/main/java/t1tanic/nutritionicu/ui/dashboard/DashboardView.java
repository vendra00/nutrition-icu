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
import com.vaadin.flow.router.PageTitle;
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

/** Landing overview: headline metrics, cohort breakdown charts and the most recent alerts. */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard · ICU Nutrition")
@PermitAll
public class DashboardView extends VerticalLayout {

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
        add(new H2("Dashboard"));

        add(row(
                statCard("Monitored patients", String.valueOf(s.monitoredPatients()),
                        "var(--lumo-primary-text-color)"),
                statCard("Active alerts", String.valueOf(s.activeAlerts()), s.activeAlerts() > 0 ? RED : GREEN),
                statCard("High NUTRIC risk", String.valueOf(s.highRisk()), s.highRisk() > 0 ? RED : GREEN),
                statCard("Avg delivery", s.avgPercentDelivered() == null ? "—" : s.avgPercentDelivered() + "%",
                        deliveryColor(s.avgPercentDelivered()))));

        add(row(
                chartCard("Nutritional risk (NUTRIC)", new Donut(List.of(
                        new Donut.Slice("High risk", s.highRisk(), RED),
                        new Donut.Slice("Low risk", s.lowRisk(), GREEN),
                        new Donut.Slice("Not assessed", s.notAssessed(), GREY)),
                        String.valueOf(s.monitoredPatients()))),
                chartCard("BMI distribution", new BarList(List.of(
                        new BarList.Bar("Underweight", s.underweight(), AMBER),
                        new BarList.Bar("Normal", s.normalWeight(), GREEN),
                        new BarList.Bar("Overweight", s.overweight(), ORANGE),
                        new BarList.Bar("Obese", s.obese(), RED)))),
                chartCard("Alerts by severity", new Donut(List.of(
                        new Donut.Slice("Critical", s.criticalAlerts(), RED),
                        new Donut.Slice("Warning", s.warningAlerts(), ORANGE)),
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
                .setHeader("Severity").setAutoWidth(true);
        Grid.Column<AlertSummary> patientCol = grid.addColumn(AlertSummary::patientMrn)
                .setHeader("Patient (NHC)").setAutoWidth(true);
        grid.addColumn(AlertSummary::sectors).setHeader("Sectors").setAutoWidth(true);
        Grid.Column<AlertSummary> detailsCol = grid.addColumn(AlertSummary::message)
                .setHeader("Details").setFlexGrow(3);
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
        severity.setItemLabelGenerator(AlertSeverity::name);
        severity.setPlaceholder("All");
        severity.setClearButtonVisible(true);
        severity.setWidthFull();
        TextField patient = filterField("NHC…");
        TextField details = filterField("Search…");

        Runnable apply = () -> {
            alertFilter = new AlertFilter(severity.getValue(),
                    blankToNull(patient.getValue()), blankToNull(details.getValue()));
            dataProvider.refreshAll();
        };
        severity.addValueChangeListener(e -> apply.run());
        patient.addValueChangeListener(e -> apply.run());
        details.addValueChangeListener(e -> apply.run());
        filterRow.getCell(severityCol).setComponent(severity);
        filterRow.getCell(patientCol).setComponent(patient);
        filterRow.getCell(detailsCol).setComponent(details);

        H3 heading = new H3("Recent alerts");
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

    private static Span severityBadge(AlertSummary alert) {
        Span badge = new Span(alert.severity());
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
