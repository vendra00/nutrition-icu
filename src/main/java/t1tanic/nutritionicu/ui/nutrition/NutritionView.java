package t1tanic.nutritionicu.ui.nutrition;
import t1tanic.nutritionicu.ui.common.BmiBadge;
import t1tanic.nutritionicu.ui.common.UiFormat;
import t1tanic.nutritionicu.ui.common.TrendChart;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import t1tanic.nutritionicu.dto.NutritionMetrics;
import t1tanic.nutritionicu.model.NutritionRiskAssessment;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.WeightMeasurement;
import t1tanic.nutritionicu.model.enums.NutricBand;
import t1tanic.nutritionicu.repo.LabResultRepository;
import t1tanic.nutritionicu.repo.PatientRepository;
import t1tanic.nutritionicu.service.NutritionService;

/**
 * Nutrition-protocol screen: pick a patient to see screening anthropometry, the
 * derived metrics (BMI, ideal/adjusted body weight, weight-loss %) and the weight
 * trend, with the edit actions in one place.
 */
@Route(value = "nutrition", layout = MainLayout.class)
@PageTitle("Nutrition · ICU Nutrition")
public class NutritionView extends VerticalLayout {

    private final transient PatientRepository patientRepository;
    private final transient NutritionService nutritionService;
    private final transient LabResultRepository labResultRepository;
    private final ComboBox<Patient> patientBox = new ComboBox<>("Patient");
    private final VerticalLayout details = new VerticalLayout();

    public NutritionView(PatientRepository patientRepository, NutritionService nutritionService,
                         LabResultRepository labResultRepository) {
        this.patientRepository = patientRepository;
        this.nutritionService = nutritionService;
        this.labResultRepository = labResultRepository;
        setWidthFull();
        setPadding(true);

        patientBox.setItems(patientRepository.findAll());
        patientBox.setItemLabelGenerator(p -> p.getFullName() + " (" + p.getMedicalRecordNumber() + ")");
        patientBox.addValueChangeListener(e -> render(e.getValue()));

        details.setPadding(false);
        details.setSpacing(false);
        details.getStyle().set("gap", "var(--lumo-space-s)");

        // One outer card enveloping the whole protocol; the per-section accordions inside are flattened.
        Div card = new Div(new H2("Nutrition protocol"), patientBox, details);
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

        Button bodyData = new Button("Edit body data", e ->
                new PatientAnthropometryEditor(patient, nutritionService, () -> reload(patient.getId())).open());
        Button weights = new Button("Manage weights", e -> {
            WeightHistoryDialog dialog = new WeightHistoryDialog(patient, nutritionService);
            dialog.addOpenedChangeListener(ev -> {
                if (!ev.isOpened()) {
                    reload(patient.getId());
                }
            });
            dialog.open();
        });

        details.add(panel("Anthropometry & metrics",
                metricsTable(patient, m), new HorizontalLayout(bodyData, weights)));
        details.add(panel("Nutritional risk (NUTRIC)", riskPanel(patient)));
        details.add(panel("Metabolic monitoring (lab)",
                new MetabolicMonitorPanel(patient, labResultRepository)));
        details.add(panel("Weight trend", weightTrend(patient)));
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
        return new TrendChart(points, null, null, "kg");
    }

    /** Patient anthropometry and derived metrics, as a compact Metric/Value table. */
    private Grid<MetricRow> metricsTable(Patient patient, NutritionMetrics m) {
        List<MetricRow> rows = List.of(
                new MetricRow("Sex", String.valueOf(patient.getSex()), null),
                new MetricRow("Age", UiFormat.ageYears(patient), null),
                new MetricRow("Height", UiFormat.number(patient.getHeightCm()) + " cm", null),
                new MetricRow("Current weight", UiFormat.number(patient.getCurrentWeightKg()) + " kg", null),
                new MetricRow("Usual weight", UiFormat.number(patient.getUsualWeightKg()) + " kg", null),
                new MetricRow("BMI", UiFormat.number(m.bmi()), m.bmi()),
                new MetricRow("Ideal body weight", UiFormat.number(m.idealBodyWeightKg()) + " kg", null),
                new MetricRow("Adjusted body weight", UiFormat.number(m.adjustedBodyWeightKg()) + " kg", null),
                new MetricRow("Recent weight loss", UiFormat.number(m.weightLossPercent()) + " %", null));

        Grid<MetricRow> grid = new Grid<>();
        grid.addColumn(MetricRow::metric).setHeader("Metric").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(MetricRow::valueComponent).setHeader("Value").setAutoWidth(true).setFlexGrow(1);
        grid.setItems(rows);
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setWidth("32em");
        return grid;
    }

    /** A metric row; {@code bmi} is non-null only for the BMI row, which renders a coloured pill. */
    private record MetricRow(String metric, String value, Double bmi) {

        Span valueComponent() {
            return bmi == null ? new Span(value) : BmiBadge.of(bmi, value);
        }
    }

    private VerticalLayout riskPanel(Patient patient) {
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(false);
        panel.setSpacing(false);

        Optional<NutritionRiskAssessment> latest = nutritionService.latestRiskAssessment(patient.getId());
        if (latest.isPresent()) {
            NutritionRiskAssessment a = latest.get();
            FormLayout form = new FormLayout();
            form.addFormItem(new Span(a.getNutricScore() + " / " + a.getNutricMax()), "NUTRIC score");
            Span risk = new Span(a.isHighRisk() ? "High risk" : "Low risk");
            risk.getElement().getThemeList().add(a.isHighRisk() ? "badge error" : "badge success");
            form.addFormItem(risk, "Risk");
            form.addFormItem(new Span(bandText(a.getApacheBand())), "APACHE II");
            form.addFormItem(new Span(bandText(a.getSofaBand())), "SOFA");
            form.addFormItem(new Span(bandText(a.getComorbidityBand())), "Comorbidities");
            form.addFormItem(new Span(bandText(a.getAdmissionDelayBand())), "Days → ICU");
            form.addFormItem(new Span(bandText(a.getIl6Band())), "IL-6");
            form.addFormItem(new Span(UiFormat.date(a.getAssessedOn())), "Assessed");
            panel.add(form);
        } else {
            panel.add(new Span("No risk assessment yet."));
        }

        Button assess = new Button("Assess risk", e ->
                new RiskAssessmentDialog(patient, nutritionService, () -> reload(patient.getId())).open());
        panel.add(assess);
        return panel;
    }

    /** Re-fetches the patient (after an edit) and re-renders. */
    private void reload(Long patientId) {
        patientRepository.findById(patientId).ifPresent(this::render);
    }

    private static String bandText(NutricBand band) {
        return band == null ? UiFormat.EMPTY : band.label();
    }
}
