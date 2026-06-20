package t1tanic.nutritionicu.ui.nutrition;
import t1tanic.nutritionicu.ui.common.UiFormat;
import t1tanic.nutritionicu.ui.common.TrendChart;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
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
        setSizeFull();
        setPadding(true);
        add(new H2("Nutrition protocol"));

        patientBox.setItems(patientRepository.findAll());
        patientBox.setItemLabelGenerator(p -> p.getFullName() + " (" + p.getMedicalRecordNumber() + ")");
        patientBox.addValueChangeListener(e -> render(e.getValue()));
        add(patientBox);

        details.setPadding(false);
        add(details);
    }

    private void render(Patient patient) {
        details.removeAll();
        if (patient == null) {
            return;
        }
        NutritionMetrics m = nutritionService.metricsFor(patient);

        FormLayout info = new FormLayout();
        info.addFormItem(new Span(String.valueOf(patient.getSex())), "Sex");
        info.addFormItem(new Span(UiFormat.ageYears(patient)), "Age");
        info.addFormItem(new Span(UiFormat.number(patient.getHeightCm()) + " cm"), "Height");
        info.addFormItem(new Span(UiFormat.number(patient.getCurrentWeightKg()) + " kg"), "Current weight");
        info.addFormItem(new Span(UiFormat.number(patient.getUsualWeightKg()) + " kg"), "Usual weight");
        info.addFormItem(new Span(UiFormat.number(m.bmi())), "BMI");
        info.addFormItem(new Span(UiFormat.number(m.idealBodyWeightKg()) + " kg"), "Ideal body weight");
        info.addFormItem(new Span(UiFormat.number(m.adjustedBodyWeightKg()) + " kg"), "Adjusted body weight");
        info.addFormItem(new Span(UiFormat.number(m.weightLossPercent()) + " %"), "Recent weight loss");
        details.add(info);

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
        details.add(new HorizontalLayout(bodyData, weights));

        details.add(new H3("Nutritional risk (NUTRIC)"));
        details.add(riskPanel(patient));

        details.add(new H3("Metabolic monitoring (lab)"));
        details.add(new MetabolicMonitorPanel(patient, labResultRepository));

        details.add(new H3("Weight trend"));
        List<WeightMeasurement> history = nutritionService.weightHistory(patient.getId());
        List<TrendChart.Point> points = history.stream()
                .filter(w -> w.getWeightKg() != null)
                .map(w -> new TrendChart.Point(
                        w.getMeasuredOn().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        w.getWeightKg()))
                .toList();
        details.add(new TrendChart(points, null, null, "kg"));
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
            form.addFormItem(new Span(a.getAssessedOn().toString()), "Assessed");
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
