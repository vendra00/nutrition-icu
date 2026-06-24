package t1tanic.nutritionicu.ui.dashboard;
import t1tanic.nutritionicu.ui.MainLayout;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder;
import com.github.appreciated.apexcharts.config.builder.LegendBuilder;
import com.github.appreciated.apexcharts.config.builder.PlotOptionsBuilder;
import com.github.appreciated.apexcharts.config.builder.XAxisBuilder;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.legend.Position;
import com.github.appreciated.apexcharts.config.plotoptions.builder.BarBuilder;
import com.github.appreciated.apexcharts.config.xaxis.XAxisType;
import com.github.appreciated.apexcharts.helper.Coordinate;
import com.github.appreciated.apexcharts.helper.Series;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import t1tanic.nutritionicu.dto.HeightWeightPoint;
import t1tanic.nutritionicu.model.enums.AlertSeverity;
import t1tanic.nutritionicu.model.enums.Sex;
import t1tanic.nutritionicu.service.alert.AlertService;
import t1tanic.nutritionicu.service.dashboard.DashboardService;
import t1tanic.nutritionicu.service.patient.PatientOverviewService;
import t1tanic.nutritionicu.ui.alerts.AlertDetailDialog;
import t1tanic.nutritionicu.ui.common.I18n;

/** Landing overview: headline counts, cohort charts (ApexCharts) and the most recent alerts. */
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
    private static final String BLUE = "#1565C0";
    private static final String PINK = "#C2185B";

    private final transient AlertService alertService;
    private final transient PatientOverviewService overviewService;
    private AlertFilter alertFilter = AlertFilter.empty();

    public DashboardView(DashboardService dashboardService, AlertService alertService,
                         PatientOverviewService overviewService) {
        this.alertService = alertService;
        this.overviewService = overviewService;
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
                chartCard(getTranslation("dashboard.nutricrisk"), nutricPie(s)),
                chartCard(getTranslation("dashboard.bmidist"), bmiColumn(s)),
                chartCard(getTranslation("dashboard.heightweight"),
                        heightWeightScatter(dashboardService.heightWeightScatter()))));

        add(recentAlertsCard());
    }

    // --- ApexCharts (community add-on) ---

    /** NUTRIC risk distribution as a pie. */
    private ApexCharts nutricPie(DashboardStats s) {
        ApexCharts pie = ApexChartsBuilder.get()
                .withChart(ChartBuilder.get().withType(Type.PIE).build())
                .withLabels(getTranslation("risk.high"), getTranslation("risk.low"),
                        getTranslation("risk.notassessed"))
                .withColors(RED, GREEN, GREY)
                .withLegend(LegendBuilder.get().withPosition(Position.BOTTOM).build())
                .withSeries((double) s.highRisk(), (double) s.lowRisk(), (double) s.notAssessed())
                .build();
        return sized(pie);
    }

    /** BMI band distribution as a coloured column chart (one bar per WHO band). */
    private ApexCharts bmiColumn(DashboardStats s) {
        Series<Double> series = new Series<>(getTranslation("dashboard.patients"),
                (double) s.underweight(), (double) s.normalWeight(),
                (double) s.overweight(), (double) s.obese());
        ApexCharts bar = ApexChartsBuilder.get()
                .withChart(ChartBuilder.get().withType(Type.BAR).build())
                .withPlotOptions(PlotOptionsBuilder.get()
                        .withBar(BarBuilder.get().withDistributed(true).withColumnWidth("55%").build()).build())
                .withColors(AMBER, GREEN, ORANGE, RED)
                .withDataLabels(DataLabelsBuilder.get().withEnabled(true).build())
                .withLegend(LegendBuilder.get().withShow(false).build())
                .withSeries(series)
                .withXaxis(XAxisBuilder.get().withCategories(
                        getTranslation("bmi.underweight"), getTranslation("bmi.normal"),
                        getTranslation("bmi.overweight"), getTranslation("bmi.obese")).build())
                .build();
        return sized(bar);
    }

    /** Height (cm) vs weight (kg) of monitored patients, one series per sex. */
    private ApexCharts heightWeightScatter(List<HeightWeightPoint> points) {
        ApexCharts scatter = ApexChartsBuilder.get()
                .withChart(ChartBuilder.get().withType(Type.SCATTER).build())
                .withColors(BLUE, PINK)
                .withLegend(LegendBuilder.get().withPosition(Position.BOTTOM).build())
                .withXaxis(XAxisBuilder.get().withType(XAxisType.NUMERIC).build())
                .withSeries(
                        sexSeries(getTranslation("sex.MALE"), points, Sex.MALE),
                        sexSeries(getTranslation("sex.FEMALE"), points, Sex.FEMALE))
                .build();
        return sized(scatter);
    }

    @SuppressWarnings("unchecked")
    private static Series<Coordinate<Double, Double>> sexSeries(String name, List<HeightWeightPoint> all, Sex sex) {
        Coordinate<Double, Double>[] data = all.stream()
                .filter(p -> p.sex() == sex)
                .map(p -> new Coordinate<>(p.heightCm(), p.weightKg()))
                .toArray(Coordinate[]::new);
        Series<Coordinate<Double, Double>> series = new Series<>();
        series.setName(name);
        series.setData(data);
        return series;
    }

    private static ApexCharts sized(ApexCharts chart) {
        chart.setWidth("100%");
        chart.setHeight("300px");
        return chart;
    }

    // --- recent alerts ---

    private Component recentAlertsCard() {
        Grid<AlertSummary> grid = new Grid<>(AlertSummary.class, false);
        Grid.Column<AlertSummary> severityCol = grid.addComponentColumn(DashboardView::severityBadge)
                .setHeader(getTranslation("alerts.col.severity")).setAutoWidth(true);
        Grid.Column<AlertSummary> patientCol = grid.addComponentColumn(this::patientLink)
                .setHeader(getTranslation("alerts.col.patient")).setAutoWidth(true).setFlexGrow(1);
        grid.setHeight("420px");

        // Lazy loading: the grid fetches pages from the backend as it scrolls, not all at once.
        CallbackDataProvider<AlertSummary, Void> dataProvider = DataProvider.fromCallbacks(
                query -> alertService.search(alertFilter, query.getOffset(), query.getLimit()).stream(),
                query -> (int) alertService.count(alertFilter));
        grid.setItems(dataProvider);

        // Header filters: severity + NHC. Changing either re-queries the backend.
        HeaderRow filterRow = grid.appendHeaderRow();
        ComboBox<AlertSeverity> severity = new ComboBox<>();
        severity.setItems(AlertSeverity.values());
        severity.setItemLabelGenerator(v -> getTranslation("alertSeverity." + v.name()));
        severity.setPlaceholder(getTranslation("filter.all"));
        severity.setClearButtonVisible(true);
        severity.setWidthFull();
        TextField patient = filterField(getTranslation("filter.nhc"));

        Runnable apply = () -> {
            alertFilter = new AlertFilter(severity.getValue(), null, blankToNull(patient.getValue()), null);
            dataProvider.refreshAll();
        };
        severity.addValueChangeListener(e -> apply.run());
        patient.addValueChangeListener(e -> apply.run());
        filterRow.getCell(severityCol).setComponent(severity);
        filterRow.getCell(patientCol).setComponent(patient);

        H3 heading = new H3(getTranslation("dashboard.recentalerts"));
        heading.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.NONE);
        VerticalLayout card = new VerticalLayout(heading, grid);
        card.setPadding(true);
        card.setWidthFull();
        decorate(card);
        return card;
    }

    /** The patient NHC as a link that opens the full alert details. */
    private Component patientLink(AlertSummary alert) {
        Button link = new Button(alert.patientMrn(), e -> new AlertDetailDialog(alert, overviewService).open());
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        return link;
    }

    private static Span severityBadge(AlertSummary alert) {
        Span badge = new Span(I18n.t("alertSeverity." + alert.severity()));
        badge.getElement().getThemeList().add("badge " + ("CRITICAL".equals(alert.severity()) ? "error" : "warning"));
        return badge;
    }

    // --- layout helpers ---

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
        card.getStyle().set("flex", "1").set("min-width", "320px");
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
