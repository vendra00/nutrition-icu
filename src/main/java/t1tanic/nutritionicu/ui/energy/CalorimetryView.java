package t1tanic.nutritionicu.ui.energy;
import t1tanic.nutritionicu.ui.common.MetricsTable;
import t1tanic.nutritionicu.ui.common.TrendChart;
import t1tanic.nutritionicu.ui.common.UiFormat;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import t1tanic.nutritionicu.dto.EnergyExpenditureResult;
import t1tanic.nutritionicu.dto.NutritionMetrics;
import t1tanic.nutritionicu.model.EnergyAssessment;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.enums.EnergyMethod;
import t1tanic.nutritionicu.model.enums.Sex;
import t1tanic.nutritionicu.service.nutrition.EnergyAssessmentService;
import t1tanic.nutritionicu.service.nutrition.NutritionFormulary;
import t1tanic.nutritionicu.service.nutrition.NutritionRegimenCalculator;
import t1tanic.nutritionicu.service.nutrition.NutritionService;
import t1tanic.nutritionicu.service.patient.PatientService;

/**
 * Indirect-calorimetry panel: the doctor records the measured energy expenditure (mEE/REE, kcal/day)
 * and respiratory quotient (RQ) read off the calorimeter. Studies are saved as {@link EnergyAssessment}s
 * (method = indirect calorimetry) so they trend over time and compare against Harris-Benedict; the latest
 * measured value drives the shared {@link NutritionRegimenPanel}. Hosted as a tab inside {@link EnergyView}.
 */
public class CalorimetryView extends VerticalLayout {

    private static final EnergyMethod METHOD = EnergyMethod.INDIRECT_CALORIMETRY;

    private final transient PatientService patientService;
    private final transient NutritionService nutritionService;
    private final transient EnergyAssessmentService energyService;

    private final ComboBox<Patient> patientBox = new ComboBox<>("Patient");
    private final Span patientPrompt = new Span("Select a patient to record and see their studies.");
    private final Grid<MetricsTable.Row> patientGrid = MetricsTable.create("Patient data");

    private final DatePicker date = new DatePicker("Date");
    private final NumberField measuredKcal = new NumberField("Measured EE (kcal/day)");
    private final NumberField rq = new NumberField("RQ");
    private final Button addOrUpdate = new Button("Add / update");

    private final Div latestHolder = new Div();
    private final Grid<EnergyAssessment> historyGrid = new Grid<>(EnergyAssessment.class, false);
    private final Div chartHolder = new Div();
    private final NutritionRegimenPanel regimenPanel;

    private Patient selectedPatient;

    public CalorimetryView(PatientService patientService,
                           NutritionService nutritionService,
                           EnergyAssessmentService energyService,
                           NutritionRegimenCalculator regimenCalculator,
                           NutritionFormulary formulary) {
        this.patientService = patientService;
        this.nutritionService = nutritionService;
        this.energyService = energyService;
        this.regimenPanel = new NutritionRegimenPanel(regimenCalculator, formulary);
        this.regimenPanel.setNoEnergyPrompt("Record a calorimetry study (the patient also needs "
                + "height and weight on file) to see the administration plan.");
        setWidthFull();
        setPadding(false);

        add(inputsPanel(), studiesPanel(), regimenPanel);
        selectPatient(null);
    }

    private Details inputsPanel() {
        patientBox.setItems(patientService.findAll());
        patientBox.setItemLabelGenerator(p -> p.getFullName() + " (" + p.getMedicalRecordNumber() + ")");
        patientBox.addValueChangeListener(e -> selectPatient(e.getValue()));

        patientGrid.setWidth("34em");
        patientGrid.setVisible(false);

        VerticalLayout content = new VerticalLayout(patientBox, patientPrompt, patientGrid);
        content.setPadding(false);
        Details panel = new Details("Patient", content);
        panel.setOpened(true);
        return panel;
    }

    private Details studiesPanel() {
        UiFormat.dayMonthYear(date);
        date.setValue(LocalDate.now());
        date.setMax(LocalDate.now());
        measuredKcal.setStep(10);
        measuredKcal.setMin(0);
        rq.setStep(0.01);
        rq.setMin(0);
        rq.setMax(2);
        rq.setHelperText("VCO₂/VO₂ · 0.85 ≈ balanced");
        addOrUpdate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addOrUpdate.addClickListener(e -> save());
        HorizontalLayout form = new HorizontalLayout(date, measuredKcal, rq, addOrUpdate);
        form.setAlignItems(FlexComponent.Alignment.BASELINE);

        historyGrid.addColumn(m -> UiFormat.date(m.getAssessedOn())).setHeader("Date").setAutoWidth(true);
        historyGrid.addColumn(m -> m.getTotalKcalPerDay() + " kcal/day")
                .setHeader("Measured EE").setAutoWidth(true);
        historyGrid.addComponentColumn(m -> rqBadge(m.getRq())).setHeader("RQ").setAutoWidth(true);
        historyGrid.addComponentColumn(m -> new Button("Delete", e -> {
            energyService.delete(m.getId());
            refresh();
        })).setHeader("").setAutoWidth(true);
        historyGrid.setAllRowsVisible(true);

        chartHolder.setWidthFull();

        VerticalLayout content = new VerticalLayout(form, latestHolder, historyGrid, chartHolder);
        content.setPadding(false);
        content.getStyle().set("gap", "var(--lumo-space-m)");
        Details panel = new Details("Calorimetry studies", content);
        panel.setOpened(true);
        return panel;
    }

    private void selectPatient(Patient patient) {
        selectedPatient = patient;
        boolean has = patient != null;
        date.setEnabled(has);
        measuredKcal.setEnabled(has);
        rq.setEnabled(has);
        addOrUpdate.setEnabled(has);
        refresh();
    }

    private void save() {
        Patient patient = currentPatient();
        if (patient == null || date.getValue() == null || measuredKcal.getValue() == null) {
            return;
        }
        int kcal = (int) Math.round(measuredKcal.getValue());
        energyService.recordCalorimetry(patient.getId(), date.getValue(), kcal, rq.getValue());
        measuredKcal.clear();
        rq.clear();
        refresh();
    }

    /** Re-reads the selected patient so anthropometry edits in the Patients tab show up. */
    private Patient currentPatient() {
        return selectedPatient == null
                ? null
                : patientService.findById(selectedPatient.getId()).orElse(null);
    }

    private void refresh() {
        Patient patient = currentPatient();
        patientPrompt.setVisible(patient == null);
        patientGrid.setVisible(patient != null);
        latestHolder.removeAll();
        chartHolder.removeAll();
        if (patient == null) {
            historyGrid.setItems(List.of());
            regimenPanel.clear();
            return;
        }

        patientGrid.setItems(patientRows(patient));
        List<EnergyAssessment> history = energyService.history(patient.getId(), METHOD);
        historyGrid.setItems(history);

        List<TrendChart.Point> points = history.stream()
                .map(m -> new TrendChart.Point(
                        m.getAssessedOn().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        (double) m.getTotalKcalPerDay()))
                .toList();
        chartHolder.add(new TrendChart(points, null, null, "kcal/day"));

        renderLatest(patient, history);
        updateRegimen(patient);
    }

    private void renderLatest(Patient patient, List<EnergyAssessment> history) {
        if (history.isEmpty()) {
            Span none = new Span("No calorimetry studies recorded yet.");
            none.addClassNames(LumoUtility.TextColor.SECONDARY);
            latestHolder.add(none);
            return;
        }
        EnergyAssessment latest = history.get(history.size() - 1);
        Double weight = patient.getCurrentWeightKg();
        String perKg = weight != null && weight > 0
                ? " · " + UiFormat.number(latest.getTotalKcalPerDay() / weight) + " kcal/kg/day"
                : "";
        Span badge = new Span("Latest: %d kcal/day%s  (%s)".formatted(
                latest.getTotalKcalPerDay(), perKg, UiFormat.date(latest.getAssessedOn())));
        badge.getElement().getThemeList().add("badge primary");
        badge.addClassName(LumoUtility.FontSize.LARGE);
        badge.getStyle().set("white-space", "normal").set("margin-right", "var(--lumo-space-s)");
        latestHolder.add(badge, rqBadge(latest.getRq()));
    }

    /** Builds an energy result from the latest measured value + patient anthropometry to feed the regimen. */
    private void updateRegimen(Patient patient) {
        NutritionMetrics m = nutritionService.metricsFor(patient);
        Double bmi = m.bmi();
        Double idealKg = m.idealBodyWeightKg();
        Double weight = patient.getCurrentWeightKg();
        var latest = energyService.latest(patient.getId(), METHOD);
        boolean ready = latest.isPresent() && bmi != null && weight != null && weight > 0
                && (bmi < 30 || idealKg != null);
        if (!ready) {
            regimenPanel.clear();
            return;
        }
        int kcal = latest.get().getTotalKcalPerDay();
        EnergyExpenditureResult energy = new EnergyExpenditureResult(
                bmi, null, idealKg != null ? idealKg : 0.0, weight, 0, kcal, kcal / weight, null);
        regimenPanel.update(energy, weight);
    }

    private static List<MetricsTable.Row> patientRows(Patient p) {
        return List.of(
                new MetricsTable.Row("Sex", sexText(p.getSex())),
                new MetricsTable.Row("Age", UiFormat.ageYears(p)),
                new MetricsTable.Row("Height", UiFormat.number(p.getHeightCm()) + " cm"),
                new MetricsTable.Row("Weight (current)", UiFormat.number(p.getCurrentWeightKg()) + " kg"));
    }

    private static String sexText(Sex sex) {
        return switch (sex) {
            case MALE -> "Male";
            case FEMALE -> "Female";
            case UNKNOWN -> UiFormat.EMPTY;
        };
    }

    /** RQ as a coloured pill with its over/under-feeding reading (ESPEN guidance). */
    private static Span rqBadge(Double value) {
        if (value == null) {
            return new Span(UiFormat.EMPTY);
        }
        String label;
        String bg;
        String fg;
        if (value < 0.70) {
            label = "underfeeding";
            bg = "#FCE4E4";
            fg = "#C62828";
        } else if (value <= 0.90) {
            label = "balanced";
            bg = "#E6F4EA";
            fg = "#2E7D32";
        } else if (value <= 1.00) {
            label = "possible overfeeding";
            bg = "#FFEBD6";
            fg = "#E65100";
        } else {
            label = "overfeeding";
            bg = "#FCE4E4";
            fg = "#C62828";
        }
        Span pill = new Span(String.format(Locale.US, "%.2f", value) + " · " + label);
        pill.getStyle().set("background-color", bg).set("color", fg)
                .set("padding", "0.1em 0.6em").set("border-radius", "var(--lumo-border-radius-s)")
                .set("font-weight", "500").set("white-space", "nowrap");
        return pill;
    }
}
