package t1tanic.nutritionicu.ui.energy;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import t1tanic.nutritionicu.dto.EnergyExpenditureResult;
import t1tanic.nutritionicu.dto.NutritionRegimen;
import t1tanic.nutritionicu.model.NutritionProduct;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.enums.NutritionCategory;
import t1tanic.nutritionicu.model.enums.Sex;
import t1tanic.nutritionicu.model.enums.StressFactor;
import t1tanic.nutritionicu.repo.PatientRepository;
import t1tanic.nutritionicu.repo.TemperatureMeasurementRepository;
import t1tanic.nutritionicu.repo.WeightMeasurementRepository;
import t1tanic.nutritionicu.service.HarrisBenedictCalculator;
import t1tanic.nutritionicu.service.NutritionFormulary;
import t1tanic.nutritionicu.service.NutritionRegimenCalculator;
import t1tanic.nutritionicu.ui.MainLayout;
import t1tanic.nutritionicu.ui.common.MetricsTable;
import t1tanic.nutritionicu.ui.common.UiFormat;

/**
 * Harris-Benedict energy-expenditure screen: pick a patient to read their sex/age/height/weight (edited
 * in the Patients tab), choose a stress degree, and see basal (GEB) and total (GET) energy expenditure.
 * Once a GET is computed, choosing a nutrition formula shows its 24-hour administration plan
 * (infusion ml/h, macros, electrolytes, protein target). Reproduces the rccc.eu calculator.
 *
 * <p>All components (badges, grids) are built once and merely re-populated/toggled as inputs change;
 * nothing is re-created per change event.
 */
@Route(value = "energy", layout = MainLayout.class)
@PageTitle("Energy expenditure · ICU Nutrition")
public class EnergyExpenditureView extends VerticalLayout {

    private final transient PatientRepository patientRepository;
    private final transient TemperatureMeasurementRepository temperatureRepository;
    private final transient WeightMeasurementRepository weightRepository;
    private final transient HarrisBenedictCalculator calculator;
    private final transient NutritionRegimenCalculator regimenCalculator;
    private final transient NutritionFormulary formulary;

    private final ComboBox<Patient> patientBox = new ComboBox<>("Patient");
    private final ComboBox<StressFactor> stressBox = new ComboBox<>("Stress degree");
    private final Span patientPrompt = new Span("Select a patient to see their data.");
    private final Grid<MetricsTable.Row> patientGrid = MetricsTable.create("Patient data");
    private final Details nutritionPanel = new Details();
    private final RadioButtonGroup<NutritionCategory> categoryBox = new RadioButtonGroup<>("Type");
    private final ComboBox<NutritionProduct> productBox = new ComboBox<>("Nutrition formula");

    /** The patient whose recorded data feeds the calculation; their data is edited in the Patients tab. */
    private Patient selectedPatient;

    // Energy result (built once)
    private final Span energyPrompt =
            new Span("Select a patient with sex, age, height and weight on file to calculate.");
    private final Span totalBadge = new Span();
    private final Grid<MetricsTable.Row> energyGrid = MetricsTable.create("Metric");

    // Nutrition regimen (built once)
    private final Span regimenPrompt = new Span();
    private final Grid<SummaryRow> summaryGrid = new Grid<>();
    private final Grid<MacroRow> macroGrid = new Grid<>();
    private final Grid<ElectrolyteRow> electrolyteGrid = new Grid<>();
    private final HorizontalLayout tables = new HorizontalLayout();

    /** Last valid energy result and the actual weight behind it, for the nutrition step. */
    private EnergyExpenditureResult lastEnergy;
    private double lastActualWeightKg;

    public EnergyExpenditureView(PatientRepository patientRepository,
                                 TemperatureMeasurementRepository temperatureRepository,
                                 WeightMeasurementRepository weightRepository,
                                 HarrisBenedictCalculator calculator,
                                 NutritionRegimenCalculator regimenCalculator,
                                 NutritionFormulary formulary) {
        this.patientRepository = patientRepository;
        this.temperatureRepository = temperatureRepository;
        this.weightRepository = weightRepository;
        this.calculator = calculator;
        this.regimenCalculator = regimenCalculator;
        this.formulary = formulary;
        setWidthFull();
        setPadding(true);
        add(new H2("Energy expenditure (Harris-Benedict)"));

        add(inputsPanel(), resultPanel(), nutritionPanel(), note());
        recompute();
    }

    // --- Panel 1: patient & inputs ---
    private Details inputsPanel() {
        patientBox.setItems(patientRepository.findAll());
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

    // --- Panel 2: energy expenditure result ---
    private Details resultPanel() {
        totalBadge.getElement().getThemeList().add("badge primary");
        totalBadge.addClassName(LumoUtility.FontSize.LARGE);
        totalBadge.getStyle().set("white-space", "normal").set("margin-bottom", "var(--lumo-space-s)");

        energyGrid.setWidth("34em");

        Details panel = new Details("Energy expenditure (GET)", new Div(energyPrompt, totalBadge, energyGrid));
        panel.setOpened(true);
        return panel;
    }

    // --- Panel 3: nutrition (hidden until a complete energy result exists) ---
    private Details nutritionPanel() {
        categoryBox.setItems(NutritionCategory.values());
        categoryBox.setItemLabelGenerator(NutritionCategory::label);
        categoryBox.setValue(NutritionCategory.ENTERAL);
        categoryBox.addValueChangeListener(e -> applyCategoryFilter());
        categoryBox.getStyle().set("margin-right", "var(--lumo-space-xl)");
        productBox.setWidth("28em");
        productBox.setItemLabelGenerator(NutritionProduct::getName);
        productBox.addValueChangeListener(e -> renderRegimen());
        applyCategoryFilter();

        configureMacroGrid();
        configureElectrolyteGrid();
        tables.add(new Div(sectionLabel("Delivered over 24 h"), macroGrid),
                new Div(sectionLabel("Electrolytes (24 h)"), electrolyteGrid));
        tables.getStyle().set("flex-wrap", "wrap").set("gap", "var(--lumo-space-l)");

        regimenPrompt.addClassName(LumoUtility.TextColor.SECONDARY);

        summaryGrid.addColumn(SummaryRow::item).setHeader("Plan").setAutoWidth(true).setFlexGrow(0);
        summaryGrid.addComponentColumn(SummaryRow::valueComponent).setHeader("Value")
                .setAutoWidth(true).setFlexGrow(1);
        summaryGrid.setAllRowsVisible(true);
        summaryGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        summaryGrid.setWidthFull();
        summaryGrid.setMaxWidth("52em");

        VerticalLayout regimen = new VerticalLayout(regimenPrompt, summaryGrid, tables);
        regimen.setPadding(false);
        regimen.setSpacing(false);
        regimen.getStyle().set("gap", "var(--lumo-space-m)");

        HorizontalLayout selectors = new HorizontalLayout(categoryBox, productBox);
        selectors.setPadding(false);
        selectors.setSpacing(true);
        selectors.getThemeList().add("spacing-xl");
        selectors.setAlignItems(FlexComponent.Alignment.START);

        VerticalLayout content = new VerticalLayout(selectors, regimen);
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("gap", "var(--lumo-space-l)").set("padding-top", "var(--lumo-space-s)");

        nutritionPanel.setSummaryText("Choose nutrition");
        nutritionPanel.add(content);
        nutritionPanel.setOpened(true);
        nutritionPanel.setVisible(false);
        return nutritionPanel;
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

    /**
     * Re-reads the selected patient from the database, so edits made in the Patients tab are reflected
     * immediately without reselecting. Cheap at this scale (a handful of monitored patients per unit).
     */
    private Patient currentPatient() {
        return selectedPatient == null
                ? null
                : patientRepository.findById(selectedPatient.getId()).orElse(null);
    }

    private List<MetricsTable.Row> patientRows(Patient p) {
        String weightValue = UiFormat.number(p.getCurrentWeightKg()) + " kg";
        MetricsTable.Row weight = weightRepository.findTopByPatientIdOrderByMeasuredOnDesc(p.getId())
                .map(w -> new MetricsTable.Row("Weight (current)", weightValue, null,
                        "Recorded " + UiFormat.date(w.getMeasuredOn())))
                .orElse(new MetricsTable.Row("Weight (current)", weightValue));
        MetricsTable.Row temperature = temperatureRepository.findTopByPatientIdOrderByMeasuredOnDesc(p.getId())
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
            lastEnergy = null;
            nutritionPanel.setVisible(false);
            renderRegimen();
            return;
        }

        EnergyExpenditureResult r = calculator.calculate(sex, weight, height, age, stress);
        lastEnergy = r;
        lastActualWeightKg = weight;
        nutritionPanel.setVisible(true);

        totalBadge.setText("GET (total): %d kcal/day".formatted(r.totalKcalPerDay()));
        energyGrid.setItems(metricRows(r));
        renderRegimen();
    }

    /** Restricts the formula dropdown to the selected category, clearing a now-hidden selection. */
    private void applyCategoryFilter() {
        NutritionCategory category = categoryBox.getValue();
        productBox.clear();
        productBox.setItems(formulary.all().stream()
                .filter(p -> p.getCategory() == category)
                .toList());
    }

    private void renderRegimen() {
        NutritionProduct product = productBox.getValue();
        if (lastEnergy == null || product == null) {
            regimenPrompt.setText(lastEnergy == null
                    ? "Calculate energy expenditure first."
                    : "Select a formula to see the administration plan.");
            setRegimenContentVisible(false);
            return;
        }
        setRegimenContentVisible(true);

        NutritionRegimen plan = regimenCalculator.calculate(lastEnergy, lastActualWeightKg, product);

        macroGrid.setItems(macroRows(plan));
        electrolyteGrid.setItems(electrolyteRows(plan.electrolytes()));
        summaryGrid.setItems(summaryRows(plan, product));
    }

    private static List<SummaryRow> summaryRows(NutritionRegimen plan, NutritionProduct product) {
        String osm = product.getOsmolarity() == null ? "" : ", " + product.getOsmolarity() + " mOsm/l";
        List<SummaryRow> rows = new ArrayList<>(List.of(
                new SummaryRow("Infusion", "%d ml/h (%d ml/day, %s kcal/ml%s)".formatted(
                        plan.infusionMlPerHour(), plan.dailyVolumeMl(),
                        UiFormat.number(product.getDensityKcalPerMl()), osm), false),
                new SummaryRow("Protein target", "%s g/day (%s g/kg of %s)".formatted(
                        UiFormat.number(plan.proteinTargetG()),
                        UiFormat.number(plan.proteinTargetPerKg()), plan.proteinBasis()), false)));
        if (plan.proteinDeficitG() > 0) {
            rows.add(new SummaryRow("Protein deficit vs target",
                    "%d g/day".formatted(plan.proteinDeficitG()), true));
        }
        if (product.getIndications() != null && !product.getIndications().isBlank()) {
            rows.add(new SummaryRow("Indications", product.getIndications(), false));
        }
        return rows;
    }

    /** Shows the prompt OR the regimen content, never both. */
    private void setRegimenContentVisible(boolean visible) {
        regimenPrompt.setVisible(!visible);
        summaryGrid.setVisible(visible);
        tables.setVisible(visible);
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

    private static List<MacroRow> macroRows(NutritionRegimen plan) {
        List<MacroRow> rows = new ArrayList<>(List.of(
                new MacroRow("Protein", plan.proteinG() + " g", plan.proteinPercent() + "%"),
                new MacroRow("Carbohydrate", plan.carbG() + " g", plan.carbPercent() + "%"),
                new MacroRow("Fat", plan.fatG() + " g", plan.fatPercent() + "%"),
                new MacroRow("Nitrogen", UiFormat.number(plan.nitrogenG()) + " g", "")));
        if (plan.fiberApplicable()) {
            rows.add(new MacroRow("Fibre", UiFormat.number(plan.fiberG()) + " g", ""));
        }
        return rows;
    }

    private static List<ElectrolyteRow> electrolyteRows(NutritionRegimen.Electrolytes el) {
        return List.of(
                new ElectrolyteRow("Sodium (Na)", UiFormat.number(el.sodiumG())),
                new ElectrolyteRow("Potassium (K)", UiFormat.number(el.potassiumG())),
                new ElectrolyteRow("Chloride (Cl)", UiFormat.number(el.chlorideG())),
                new ElectrolyteRow("Calcium (Ca)", UiFormat.number(el.calciumG())),
                new ElectrolyteRow("Magnesium (Mg)", UiFormat.number(el.magnesiumG())),
                new ElectrolyteRow("Phosphorus (P)", UiFormat.number(el.phosphorusG())));
    }

    private void configureMacroGrid() {
        macroGrid.addColumn(MacroRow::nutrient).setHeader("Nutrient").setAutoWidth(true).setFlexGrow(1);
        macroGrid.addColumn(MacroRow::amount).setHeader("Amount / 24 h")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        macroGrid.addColumn(MacroRow::share).setHeader("% kcal")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        macroGrid.setAllRowsVisible(true);
        macroGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        macroGrid.setWidth("28em");
    }

    private void configureElectrolyteGrid() {
        electrolyteGrid.addColumn(ElectrolyteRow::name).setHeader("Electrolyte")
                .setAutoWidth(true).setFlexGrow(1);
        electrolyteGrid.addColumn(ElectrolyteRow::amount).setHeader("g / 24 h")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        electrolyteGrid.setAllRowsVisible(true);
        electrolyteGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        electrolyteGrid.setWidth("22em");
    }

    /** A bold sub-heading for a block within the regimen output. */
    private static Span sectionLabel(String text) {
        Span span = new Span(text);
        span.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        span.getStyle().set("display", "block").set("margin-bottom", "var(--lumo-space-xs)");
        return span;
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

    /** A metric row; {@code bmi} is non-null only for the BMI row, which renders a coloured pill. */
    /** A regimen-summary row; {@code warn} renders the value in the error colour (e.g. protein deficit). */
    private record SummaryRow(String item, String value, boolean warn) {

        Span valueComponent() {
            Span span = new Span(value);
            span.getStyle().set("white-space", "normal");
            if (warn) {
                span.getStyle().set("color", "var(--lumo-error-text-color)").set("font-weight", "500");
            }
            return span;
        }
    }

    private record MacroRow(String nutrient, String amount, String share) {
    }

    private record ElectrolyteRow(String name, String amount) {
    }
}
