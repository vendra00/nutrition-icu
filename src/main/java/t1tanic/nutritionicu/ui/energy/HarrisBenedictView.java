package t1tanic.nutritionicu.ui.energy;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.time.LocalDate;
import java.util.List;
import t1tanic.nutritionicu.dto.EnergyExpenditureResult;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.enums.Sex;
import t1tanic.nutritionicu.model.enums.StressFactor;
import t1tanic.nutritionicu.service.nutrition.HarrisBenedictCalculator;
import t1tanic.nutritionicu.service.nutrition.NutritionFormulary;
import t1tanic.nutritionicu.service.nutrition.NutritionRegimenCalculator;
import t1tanic.nutritionicu.service.nutrition.NutritionService;
import t1tanic.nutritionicu.service.patient.PatientService;
import t1tanic.nutritionicu.ui.common.MetricsTable;
import t1tanic.nutritionicu.ui.common.UiFormat;

/**
 * Harris-Benedict energy-expenditure panel: pick a patient to read their sex/age/height/weight (edited
 * in the Patients tab), choose a stress degree, and see basal (GEB) and total (GET) energy expenditure.
 * Once a GET is computed, the shared {@link NutritionRegimenPanel} shows the 24-hour administration plan.
 * Reproduces the rccc.eu calculator. Hosted as a tab inside {@link EnergyView}.
 */
public class HarrisBenedictView extends VerticalLayout {

    private final transient PatientService patientService;
    private final transient NutritionService nutritionService;
    private final transient HarrisBenedictCalculator calculator;

    private final ComboBox<Patient> patientBox = new ComboBox<>("Patient");
    private final ComboBox<StressFactor> stressBox = new ComboBox<>("Stress degree");
    private final Span patientPrompt = new Span("Select a patient to see their data.");
    private final Grid<MetricsTable.Row> patientGrid = MetricsTable.create("Patient data");
    private final NutritionRegimenPanel regimenPanel;

    private final Span energyPrompt =
            new Span("Select a patient with sex, age, height and weight on file to calculate.");
    private final Span totalBadge = new Span();
    private final Grid<MetricsTable.Row> energyGrid = MetricsTable.create("Metric");

    private Patient selectedPatient;

    public HarrisBenedictView(PatientService patientService,
                              NutritionService nutritionService,
                              HarrisBenedictCalculator calculator,
                              NutritionRegimenCalculator regimenCalculator,
                              NutritionFormulary formulary) {
        this.patientService = patientService;
        this.nutritionService = nutritionService;
        this.calculator = calculator;
        this.regimenPanel = new NutritionRegimenPanel(regimenCalculator, formulary);
        this.regimenPanel.setNoEnergyPrompt("Calculate energy expenditure first.");
        setWidthFull();
        setPadding(false);

        add(inputsPanel(), resultPanel(), regimenPanel, note());
        recompute();
    }

    private Details inputsPanel() {
        patientBox.setItems(patientService.findAll());
        patientBox.setItemLabelGenerator(p -> p.getFullName() + " (" + p.getMedicalRecordNumber() + ")");
        patientBox.addValueChangeListener(e -> selectPatient(e.getValue()));

        patientGrid.setWidth("34em");
        patientGrid.setVisible(false);

        Span editHint = new Span("Sex, age, height and weight are read from the patient record — "
                + "edit them in the Patients tab.");
        editHint.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);

        stressBox.setItems(StressFactor.values());
        stressBox.setItemLabelGenerator(s -> "%s (×%.2f)".formatted(s.label(), s.factor()));
        stressBox.setValue(StressFactor.NO_STRESS);
        stressBox.addValueChangeListener(e -> recompute());

        VerticalLayout content = new VerticalLayout(
                patientBox, patientPrompt, patientGrid, editHint, stressBox);
        content.setPadding(false);
        Details panel = new Details("Patient & inputs", content);
        panel.setOpened(true);
        return panel;
    }

    private Details resultPanel() {
        totalBadge.getElement().getThemeList().add("badge primary");
        totalBadge.addClassName(LumoUtility.FontSize.LARGE);
        totalBadge.getStyle().set("white-space", "normal").set("margin-bottom", "var(--lumo-space-s)");

        energyGrid.setWidth("34em");

        Details panel = new Details("Energy expenditure (GET)", new Div(energyPrompt, totalBadge, energyGrid));
        panel.setOpened(true);
        return panel;
    }

    private Span note() {
        Span note = new Span("Decision support only. Reproduces the rccc.eu Harris-Benedict "
                + "calculator: total = basal × (0.1 activity + stress factor), with an obesity weight "
                + "adjustment above BMI 30. The infusion rate delivers GET over 24 h.");
        note.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);
        return note;
    }

    private void selectPatient(Patient patient) {
        selectedPatient = patient;
        recompute();
    }

    /** Re-reads the selected patient so edits in the Patients tab show up without reselecting. */
    private Patient currentPatient() {
        return selectedPatient == null
                ? null
                : patientService.findById(selectedPatient.getId()).orElse(null);
    }

    private List<MetricsTable.Row> patientRows(Patient p) {
        String weightValue = UiFormat.number(p.getCurrentWeightKg()) + " kg";
        MetricsTable.Row weight = nutritionService.latestWeight(p.getId())
                .map(w -> new MetricsTable.Row("Weight (current)", weightValue, null,
                        "Recorded " + UiFormat.date(w.getMeasuredOn())))
                .orElse(new MetricsTable.Row("Weight (current)", weightValue));
        MetricsTable.Row temperature = nutritionService.latestTemperature(p.getId())
                .map(t -> new MetricsTable.Row("Temperature (latest)",
                        UiFormat.number(t.getTemperatureCelsius()) + " °C", null,
                        "Recorded " + UiFormat.date(t.getMeasuredOn())))
                .orElse(new MetricsTable.Row("Temperature (latest)", UiFormat.EMPTY));
        return List.of(
                new MetricsTable.Row("Sex", sexText(p.getSex())),
                new MetricsTable.Row("Age", UiFormat.ageYears(p)),
                new MetricsTable.Row("Height", UiFormat.number(p.getHeightCm()) + " cm"),
                weight,
                temperature);
    }

    private static String sexText(Sex sex) {
        return switch (sex) {
            case MALE -> "Male";
            case FEMALE -> "Female";
            case UNKNOWN -> UiFormat.EMPTY;
        };
    }

    private void recompute() {
        Patient patient = currentPatient();
        patientPrompt.setVisible(patient == null);
        patientGrid.setVisible(patient != null);
        if (patient != null) {
            patientGrid.setItems(patientRows(patient));
        }

        Sex sex = patient == null || patient.getSex() == Sex.UNKNOWN ? null : patient.getSex();
        Integer age = patient == null ? null : patient.ageOn(LocalDate.now());
        Double height = patient == null ? null : patient.getHeightCm();
        Double weight = patient == null ? null : patient.getCurrentWeightKg();
        StressFactor stress = stressBox.getValue();

        boolean complete = sex != null && stress != null
                && age != null && age > 0 && positive(height) && positive(weight);
        energyPrompt.setVisible(!complete);
        totalBadge.setVisible(complete);
        energyGrid.setVisible(complete);
        if (!complete) {
            regimenPanel.clear();
            return;
        }

        EnergyExpenditureResult r = calculator.calculate(sex, weight, height, age, stress);
        totalBadge.setText("GET (total): %d kcal/day".formatted(r.totalKcalPerDay()));
        energyGrid.setItems(metricRows(r));
        regimenPanel.update(r, weight);
    }

    private static List<MetricsTable.Row> metricRows(EnergyExpenditureResult r) {
        return List.of(
                new MetricsTable.Row("GEB (basal)", r.basalKcalPerDay() + " kcal/day"),
                new MetricsTable.Row("Per kg (actual weight)",
                        UiFormat.number(r.kcalPerKgPerDay()) + " kcal/kg/day"),
                new MetricsTable.Row("BMI", UiFormat.number(r.bmi()), r.bmi()),
                new MetricsTable.Row("Weight class", weightBasis(r)),
                new MetricsTable.Row("Ideal body weight", UiFormat.number(r.idealBodyWeightKg()) + " kg"),
                new MetricsTable.Row("Weight used in equation", UiFormat.number(r.weightUsedKg()) + " kg"));
    }

    private static String weightBasis(EnergyExpenditureResult r) {
        Double adj = r.weightClass().adjustmentConstant();
        return adj == null
                ? r.weightClass().label()
                : "%s (adjustment %.2f)".formatted(r.weightClass().label(), adj);
    }

    private static boolean positive(Double value) {
        return value != null && value > 0;
    }
}
