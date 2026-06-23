package t1tanic.nutritionicu.ui.nutrition;
import t1tanic.nutritionicu.ui.common.I18n;
import t1tanic.nutritionicu.ui.common.MetricsTable;
import t1tanic.nutritionicu.ui.common.UiFormat;
import t1tanic.nutritionicu.ui.common.TrendChart;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import t1tanic.nutritionicu.dto.NutritionMetrics;
import t1tanic.nutritionicu.model.NutritionDelivery;
import t1tanic.nutritionicu.model.NutritionRiskAssessment;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.TemperatureMeasurement;
import t1tanic.nutritionicu.model.WeightMeasurement;
import t1tanic.nutritionicu.model.enums.NutricBand;
import t1tanic.nutritionicu.service.lab.LabResultService;
import t1tanic.nutritionicu.service.nutrition.NutritionDeliveryService;
import t1tanic.nutritionicu.service.nutrition.NutritionService;
import t1tanic.nutritionicu.service.patient.PatientService;

/**
 * Nutrition-protocol screen: pick a patient to see screening anthropometry, the
 * derived metrics (BMI, ideal/adjusted body weight, weight-loss %) and the weight
 * trend, with the edit actions in one place.
 */
@Route(value = "nutrition", layout = MainLayout.class)
@PermitAll
public class NutritionView extends VerticalLayout implements HasDynamicTitle {

    @Override
    public String getPageTitle() {
        return getTranslation("nutrition.title") + " · " + getTranslation("app.title");
    }

    /** Afebrile normal body-temperature band (°C), shown as the green zone on the temperature trend. */
    private static final double NORMAL_TEMP_LOW_C = 36.5;
    private static final double NORMAL_TEMP_HIGH_C = 37.5;
    /** Healthy BMI bounds; the weight green zone is the weight range these imply for the patient's height. */
    private static final double HEALTHY_BMI_LOW = 18.5;
    private static final double HEALTHY_BMI_HIGH = 25.0;

    private final transient PatientService patientService;
    private final transient NutritionService nutritionService;
    private final transient LabResultService labResultService;
    private final transient NutritionDeliveryService deliveryService;
    private final ComboBox<Patient> patientBox = new ComboBox<>(I18n.t("common.patient"));
    private final VerticalLayout details = new VerticalLayout();

    public NutritionView(PatientService patientService, NutritionService nutritionService,
                         LabResultService labResultService, NutritionDeliveryService deliveryService) {
        this.patientService = patientService;
        this.nutritionService = nutritionService;
        this.labResultService = labResultService;
        this.deliveryService = deliveryService;
        setWidthFull();
        setPadding(true);

        patientBox.setItems(patientService.findAll());
        patientBox.setItemLabelGenerator(p -> p.getFullName() + " (" + p.getMedicalRecordNumber() + ")");
        patientBox.addValueChangeListener(e -> render(e.getValue()));

        details.setPadding(false);
        details.setSpacing(false);
        details.getStyle().set("gap", "var(--lumo-space-s)");

        // One outer card enveloping the whole protocol; the per-section accordions inside are flattened.
        Div card = new Div(new H2(getTranslation("nutrition.title")), patientBox, details);
        card.setWidthFull();
        card.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-m)")
                .set("padding", "var(--lumo-space-l)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("background-color", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)");
        add(card);
    }

    private void render(Patient patient) {
        details.removeAll();
        if (patient == null) {
            return;
        }
        NutritionMetrics m = nutritionService.metricsFor(patient);

        Button bodyData = new Button(getTranslation("nutrition.btn.bodydata"), e ->
                new PatientAnthropometryEditor(patient, nutritionService, () -> reload(patient.getId())).open());
        Button weights = new Button(getTranslation("nutrition.btn.weights"), e -> {
            WeightHistoryDialog dialog = new WeightHistoryDialog(patient, nutritionService);
            dialog.addOpenedChangeListener(ev -> {
                if (!ev.isOpened()) {
                    reload(patient.getId());
                }
            });
            dialog.open();
        });
        Button temperature = new Button(getTranslation("nutrition.btn.temperature"), e ->
                new TemperatureHistoryDialog(patient, nutritionService).open());
        Button delivery = new Button(getTranslation("nutrition.btn.delivery"), e -> {
            NutritionDeliveryDialog dialog = new NutritionDeliveryDialog(patient, deliveryService);
            dialog.addOpenedChangeListener(ev -> {
                if (!ev.isOpened()) {
                    reload(patient.getId());
                }
            });
            dialog.open();
        });

        details.add(panel(getTranslation("nutrition.panel.anthropometry"), metricsTable(patient, m),
                new HorizontalLayout(bodyData, weights, temperature, delivery)));
        details.add(panel(getTranslation("nutrition.panel.risk"), riskPanel(patient)));
        details.add(panel(getTranslation("nutrition.panel.metabolic"),
                new MetabolicMonitorPanel(patient, labResultService)));
        details.add(panel(getTranslation("nutrition.panel.weighttrend"), weightTrend(patient)));
        details.add(panel(getTranslation("nutrition.panel.temptrend"), temperatureTrend(patient)));
        details.add(panel(getTranslation("nutrition.panel.delivery"), deliveryTrend(patient)));
    }

    /** Wraps a section's content in an open collapsible panel, matching the Energy tab's accordions. */
    private static Details panel(String title, Component... content) {
        // A block-level Div (not a flex VerticalLayout) keeps a contained Grid inside the card's
        // border; a flex parent lets the Grid overflow the Lumo Details card in Vaadin 25.
        Div body = new Div(content);
        body.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("gap", "var(--lumo-space-m)").set("padding-top", "var(--lumo-space-s)");
        Details panel = new Details(title, body);
        panel.setOpened(true);
        // Flat inside the outer card: drop the Details' own card chrome so only the outer box shows.
        panel.getStyle().set("border", "none").set("box-shadow", "none").set("background", "transparent");
        return panel;
    }

    private TrendChart weightTrend(Patient patient) {
        List<WeightMeasurement> history = nutritionService.weightHistory(patient.getId());
        List<TrendChart.Point> points = history.stream()
                .filter(w -> w.getWeightKg() != null)
                .map(w -> new TrendChart.Point(
                        w.getMeasuredOn().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        w.getWeightKg()))
                .toList();

        // Green zone = the weight range for a healthy BMI at this patient's height (when known).
        Double low = null;
        Double high = null;
        Double heightCm = patient.getHeightCm();
        if (heightCm != null && heightCm > 0) {
            double mSquared = (heightCm / 100.0) * (heightCm / 100.0);
            low = round(HEALTHY_BMI_LOW * mSquared);
            high = round(HEALTHY_BMI_HIGH * mSquared);
        }
        return new TrendChart(points, low, high, "kg");
    }

    private TrendChart temperatureTrend(Patient patient) {
        List<TemperatureMeasurement> history = nutritionService.temperatureHistory(patient.getId());
        List<TrendChart.Point> points = history.stream()
                .filter(t -> t.getTemperatureCelsius() != null)
                .map(t -> new TrendChart.Point(
                        t.getMeasuredOn().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        t.getTemperatureCelsius()))
                .toList();
        return new TrendChart(points, NORMAL_TEMP_LOW_C, NORMAL_TEMP_HIGH_C, "°C");
    }

    private TrendChart deliveryTrend(Patient patient) {
        List<NutritionDelivery> history = deliveryService.history(patient.getId());
        List<TrendChart.Point> points = history.stream()
                .filter(d -> d.percentDelivered() != null)
                .map(d -> new TrendChart.Point(
                        d.getMeasuredOn().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        d.percentDelivered()))
                .toList();
        // Green zone = 80–100% of prescribed (adequate delivery).
        return new TrendChart(points, 80.0, 100.0, getTranslation("nutrition.unit.delivered"));
    }

    /** Latest-delivery row: shows % of prescribed actually delivered, with the rates/date on hover. */
    private MetricsTable.Row deliveryRow(Patient patient) {
        String label = getTranslation("nutrition.row.delivery");
        return deliveryService.latest(patient.getId())
                .map(d -> {
                    Double pct = d.percentDelivered();
                    String value = pct == null ? UiFormat.EMPTY : getTranslation("nutrition.deliveredpct", Math.round(pct));
                    String tooltip = "%s / %s ml/h · %s".formatted(
                            UiFormat.number(d.getActualMlPerHour()),
                            UiFormat.number(d.getPrescribedMlPerHour()), UiFormat.date(d.getMeasuredOn()));
                    return new MetricsTable.Row(label, value, null, tooltip);
                })
                .orElse(new MetricsTable.Row(label, UiFormat.EMPTY));
    }

    /** Current-weight row: shows the value; the latest reading's date appears as a hover tooltip. */
    private MetricsTable.Row currentWeightRow(Patient patient) {
        String label = getTranslation("nutrition.row.currentweight");
        String value = UiFormat.number(patient.getCurrentWeightKg()) + " kg";
        return nutritionService.latestWeight(patient.getId())
                .map(w -> new MetricsTable.Row(label, value, null,
                        getTranslation("nutrition.recorded", UiFormat.date(w.getMeasuredOn()))))
                .orElse(new MetricsTable.Row(label, value));
    }

    /** Latest-temperature row: shows the value; the reading date appears as a hover tooltip. */
    private MetricsTable.Row temperatureRow(Patient patient) {
        String label = getTranslation("nutrition.row.temperature");
        return nutritionService.latestTemperature(patient.getId())
                .map(t -> new MetricsTable.Row(label,
                        UiFormat.number(t.getTemperatureCelsius()) + " °C", null,
                        getTranslation("nutrition.recorded", UiFormat.date(t.getMeasuredOn()))))
                .orElse(new MetricsTable.Row(label, UiFormat.EMPTY));
    }

    /** Patient anthropometry and derived metrics, as a compact Metric/Value table. */
    private Grid<MetricsTable.Row> metricsTable(Patient patient, NutritionMetrics m) {
        Grid<MetricsTable.Row> grid = MetricsTable.create(getTranslation("nutrition.metric"));
        grid.setItems(
                new MetricsTable.Row(getTranslation("nutrition.row.sex"),
                        patient.getSex() == null ? UiFormat.EMPTY : getTranslation("sex." + patient.getSex().name())),
                new MetricsTable.Row(getTranslation("nutrition.row.age"), UiFormat.ageYears(patient)),
                new MetricsTable.Row(getTranslation("nutrition.row.height"), UiFormat.number(patient.getHeightCm()) + " cm"),
                currentWeightRow(patient),
                new MetricsTable.Row(getTranslation("nutrition.row.usualweight"), UiFormat.number(patient.getUsualWeightKg()) + " kg"),
                temperatureRow(patient),
                deliveryRow(patient),
                new MetricsTable.Row(getTranslation("nutrition.row.bmi"), UiFormat.number(m.bmi()), m.bmi()),
                new MetricsTable.Row(getTranslation("nutrition.row.ibw"), UiFormat.number(m.idealBodyWeightKg()) + " kg"),
                new MetricsTable.Row(getTranslation("nutrition.row.abw"), UiFormat.number(m.adjustedBodyWeightKg()) + " kg"),
                new MetricsTable.Row(getTranslation("nutrition.row.weightloss"), UiFormat.number(m.weightLossPercent()) + " %"));
        grid.setWidth("32em");
        return grid;
    }

    private VerticalLayout riskPanel(Patient patient) {
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(false);
        panel.setSpacing(false);
        panel.getStyle().set("gap", "var(--lumo-space-s)");

        Optional<NutritionRiskAssessment> latest = nutritionService.latestRiskAssessment(patient.getId());
        if (latest.isPresent()) {
            NutritionRiskAssessment a = latest.get();
            Grid<MetricsTable.Row> grid = MetricsTable.create(getTranslation("risk.nutric"));
            grid.setItems(
                    new MetricsTable.Row(getTranslation("risk.score"), a.getNutricScore() + " / " + a.getNutricMax()),
                    MetricsTable.Row.badge(getTranslation("risk.risk"),
                            a.isHighRisk() ? getTranslation("risk.high") : getTranslation("risk.low"),
                            a.isHighRisk() ? "error" : "success"),
                    new MetricsTable.Row(getTranslation("risk.apache"), bandText(a.getApacheBand())),
                    new MetricsTable.Row(getTranslation("risk.sofa"), bandText(a.getSofaBand())),
                    new MetricsTable.Row(getTranslation("risk.comorbidities"), bandText(a.getComorbidityBand())),
                    new MetricsTable.Row(getTranslation("risk.daystoicu"), bandText(a.getAdmissionDelayBand())),
                    new MetricsTable.Row(getTranslation("risk.il6"), bandText(a.getIl6Band())),
                    new MetricsTable.Row(getTranslation("risk.assessed"), UiFormat.date(a.getAssessedOn())));
            grid.setWidth("32em");
            panel.add(grid);
        } else {
            panel.add(new Span(getTranslation("risk.none")));
        }

        Button assess = new Button(getTranslation("risk.assess"), e ->
                new RiskAssessmentDialog(patient, nutritionService, () -> reload(patient.getId())).open());
        panel.add(assess);
        return panel;
    }

    /** Re-fetches the patient (after an edit) and re-renders. */
    private void reload(Long patientId) {
        patientService.findById(patientId).ifPresent(this::render);
    }

    private static String bandText(NutricBand band) {
        return band == null ? UiFormat.EMPTY : band.label();
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
