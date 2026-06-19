package t1tanic.nutritionicu.ui.energy;
import t1tanic.nutritionicu.ui.common.UiFormat;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
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
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
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
import t1tanic.nutritionicu.service.HarrisBenedictCalculator;
import t1tanic.nutritionicu.service.NutritionFormulary;
import t1tanic.nutritionicu.service.NutritionRegimenCalculator;

/**
 * Harris-Benedict energy-expenditure screen: pick a patient to seed sex/age/height/weight from their
 * anthropometry, choose a stress degree, and see basal (GEB) and total (GET) energy expenditure.
 * Once a GET is computed, choosing a nutrition formula shows its 24-hour administration plan
 * (infusion ml/h, macros, electrolytes, protein target). Reproduces the rccc.eu calculator.
 */
@Route(value = "energy", layout = MainLayout.class)
@PageTitle("Energy expenditure · ICU Nutrition")
public class EnergyExpenditureView extends VerticalLayout {

    private final PatientRepository patientRepository;
    private final HarrisBenedictCalculator calculator;
    private final NutritionRegimenCalculator regimenCalculator;
    private final NutritionFormulary formulary;

    private final ComboBox<Patient> patientBox = new ComboBox<>("Patient");
    private final ComboBox<Sex> sexBox = new ComboBox<>("Sex");
    private final NumberField ageField = new NumberField("Age (years)");
    private final NumberField heightField = new NumberField("Height (cm)");
    private final NumberField weightField = new NumberField("Weight (kg)");
    private final ComboBox<StressFactor> stressBox = new ComboBox<>("Stress degree");
    private final Div results = new Div();
    private final Details nutritionPanel = new Details();
    private final RadioButtonGroup<NutritionCategory> categoryBox = new RadioButtonGroup<>("Type");
    private final ComboBox<NutritionProduct> productBox = new ComboBox<>("Nutrition formula");
    private final VerticalLayout regimenResults = new VerticalLayout();

    /** Last valid energy result and the actual weight behind it, for the nutrition step. */
    private EnergyExpenditureResult lastEnergy;
    private double lastActualWeightKg;

    public EnergyExpenditureView(PatientRepository patientRepository,
                                 HarrisBenedictCalculator calculator,
                                 NutritionRegimenCalculator regimenCalculator,
                                 NutritionFormulary formulary) {
        this.patientRepository = patientRepository;
        this.calculator = calculator;
        this.regimenCalculator = regimenCalculator;
        this.formulary = formulary;
        setWidthFull();
        setPadding(true);
        add(new H2("Energy expenditure (Harris-Benedict)"));

        // --- Panel 1: patient & inputs ---
        patientBox.setItems(patientRepository.findAll());
        patientBox.setItemLabelGenerator(p -> p.getFullName() + " (" + p.getMedicalRecordNumber() + ")");
        patientBox.addValueChangeListener(e -> prefill(e.getValue()));

        sexBox.setItems(Sex.MALE, Sex.FEMALE);
        sexBox.setItemLabelGenerator(s -> s == Sex.MALE ? "Male" : "Female");
        stressBox.setItems(StressFactor.values());
        stressBox.setItemLabelGenerator(s -> "%s (×%.2f)".formatted(s.label(), s.factor()));
        stressBox.setValue(StressFactor.NO_STRESS);

        FormLayout inputs = new FormLayout(sexBox, ageField, heightField, weightField, stressBox);
        sexBox.addValueChangeListener(e -> recompute());
        ageField.addValueChangeListener(e -> recompute());
        heightField.addValueChangeListener(e -> recompute());
        weightField.addValueChangeListener(e -> recompute());
        stressBox.addValueChangeListener(e -> recompute());

        VerticalLayout inputsContent = new VerticalLayout(patientBox, inputs);
        inputsContent.setPadding(false);
        Details inputsPanel = new Details("Patient & inputs", inputsContent);
        inputsPanel.setOpened(true);

        // --- Panel 2: energy expenditure result ---
        Details resultPanel = new Details("Energy expenditure (GET)", results);
        resultPanel.setOpened(true);

        // --- Panel 3: nutrition (hidden until a complete energy result exists) ---
        categoryBox.setItems(NutritionCategory.values());
        categoryBox.setItemLabelGenerator(NutritionCategory::label);
        categoryBox.setValue(NutritionCategory.ENTERAL);
        categoryBox.addValueChangeListener(e -> applyCategoryFilter());
        productBox.setWidth("28em");
        productBox.setItemLabelGenerator(NutritionProduct::getName);
        productBox.addValueChangeListener(e -> renderRegimen());
        applyCategoryFilter();
        regimenResults.setPadding(false);
        regimenResults.setSpacing(false);
        regimenResults.getStyle().set("gap", "var(--lumo-space-s)");

        categoryBox.getStyle().set("margin-right", "var(--lumo-space-xl)");
        HorizontalLayout selectors = new HorizontalLayout(categoryBox, productBox);
        selectors.setPadding(false);
        selectors.setSpacing(true);
        selectors.getThemeList().add("spacing-xl");
        selectors.setAlignItems(FlexComponent.Alignment.START);
        VerticalLayout nutritionContent = new VerticalLayout(selectors, regimenResults);
        nutritionContent.setPadding(false);
        nutritionContent.setSpacing(false);
        nutritionContent.getStyle().set("gap", "var(--lumo-space-l)").set("padding-top", "var(--lumo-space-s)");

        nutritionPanel.setSummaryText("Choose nutrition");
        nutritionPanel.add(nutritionContent);
        nutritionPanel.setOpened(true);
        nutritionPanel.setVisible(false);

        Span note = new Span("Decision support only. Reproduces the rccc.eu Harris-Benedict "
                + "calculator: total = basal × (0.1 activity + stress factor), with an obesity weight "
                + "adjustment above BMI 30. The infusion rate delivers GET over 24 h.");
        note.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        add(inputsPanel, resultPanel, nutritionPanel, note);
        recompute();
    }

    private void prefill(Patient patient) {
        if (patient == null) {
            return;
        }
        sexBox.setValue(patient.getSex() == Sex.UNKNOWN ? null : patient.getSex());
        Integer age = patient.ageOn(LocalDate.now());
        ageField.setValue(age == null ? null : age.doubleValue());
        heightField.setValue(patient.getHeightCm());
        weightField.setValue(patient.getCurrentWeightKg());
        recompute();
    }

    private void recompute() {
        results.removeAll();
        Sex sex = sexBox.getValue();
        Double age = ageField.getValue();
        Double height = heightField.getValue();
        Double weight = weightField.getValue();
        StressFactor stress = stressBox.getValue();

        if (sex == null || stress == null || !positive(age) || !positive(height) || !positive(weight)) {
            results.add(new Span("Enter sex, age, height and weight to calculate."));
            lastEnergy = null;
            nutritionPanel.setVisible(false);
            renderRegimen();
            return;
        }

        EnergyExpenditureResult r = calculator.calculate(
                sex, weight, height, age.intValue(), stress);
        lastEnergy = r;
        lastActualWeightKg = weight;
        nutritionPanel.setVisible(true);

        Span total = new Span("GET (total): %d kcal/day".formatted(r.totalKcalPerDay()));
        total.getElement().getThemeList().add("badge primary");
        total.getStyle().set("font-size", "var(--lumo-font-size-l)").set("white-space", "normal")
                .set("margin-bottom", "var(--lumo-space-s)");

        results.add(total, energyGrid(r));
        renderRegimen();
    }

    private record MetricRow(String metric, String value) {
    }

    /** The Harris-Benedict result as a metric/value table. */
    private static Grid<MetricRow> energyGrid(EnergyExpenditureResult r) {
        Grid<MetricRow> grid = new Grid<>();
        grid.addColumn(MetricRow::metric).setHeader("Metric").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(MetricRow::value).setHeader("Value").setAutoWidth(true);
        grid.setItems(
                new MetricRow("GEB (basal)", r.basalKcalPerDay() + " kcal/day"),
                new MetricRow("Per kg (actual weight)",
                        UiFormat.number(r.kcalPerKgPerDay()) + " kcal/kg/day"),
                new MetricRow("BMI", UiFormat.number(r.bmi())),
                new MetricRow("Weight class", weightBasis(r)),
                new MetricRow("Ideal body weight", UiFormat.number(r.idealBodyWeightKg()) + " kg"),
                new MetricRow("Weight used in equation", UiFormat.number(r.weightUsedKg()) + " kg"));
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setWidth("34em");
        return grid;
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
        regimenResults.removeAll();
        NutritionProduct product = productBox.getValue();
        if (lastEnergy == null) {
            regimenResults.add(muted("Calculate energy expenditure first."));
            return;
        }
        if (product == null) {
            regimenResults.add(muted("Select a formula to see the administration plan."));
            return;
        }

        NutritionRegimen plan = regimenCalculator.calculate(lastEnergy, lastActualWeightKg, product);

        String osm = product.getOsmolarity() == null ? "" : ", " + product.getOsmolarity() + " mOsm/l";
        Span rate = new Span("Infusion: %d ml/h  (%d ml/day, %s kcal/ml%s)".formatted(
                plan.infusionMlPerHour(), plan.dailyVolumeMl(),
                UiFormat.number(product.getDensityKcalPerMl()), osm));
        rate.getElement().getThemeList().add("badge primary");
        rate.getStyle().set("font-size", "var(--lumo-font-size-l)").set("white-space", "normal");

        Span proteinTarget = new Span(
                "Protein target: %s g/day  (%s g/kg of %s)".formatted(
                        UiFormat.number(plan.proteinTargetG()),
                        UiFormat.number(plan.proteinTargetPerKg()), plan.proteinBasis()));
        proteinTarget.getStyle().set("white-space", "normal");

        HorizontalLayout tables = new HorizontalLayout(
                new Div(sectionLabel("Delivered over 24 h"), macroGrid(plan)),
                new Div(sectionLabel("Electrolytes (24 h)"), electrolyteGrid(plan.electrolytes())));
        tables.getStyle().set("flex-wrap", "wrap").set("gap", "var(--lumo-space-l)");

        regimenResults.add(rate, tables, proteinTarget);
        if (plan.proteinDeficitG() > 0) {
            Span deficit = new Span("Protein deficit vs target: %d g/day".formatted(plan.proteinDeficitG()));
            deficit.getElement().getThemeList().add("badge error");
            deficit.getStyle().set("white-space", "normal");
            regimenResults.add(new Div(deficit));
        }
        if (product.getIndications() != null && !product.getIndications().isBlank()) {
            regimenResults.add(muted("Indications: " + product.getIndications()));
        }
    }

    private record MacroRow(String nutrient, String amount, String share) {
    }

    private record ElectrolyteRow(String name, String amount) {
    }

    /** Macronutrients delivered over 24 h: amount and share of calories. */
    private static Grid<MacroRow> macroGrid(NutritionRegimen plan) {
        List<MacroRow> rows = new ArrayList<>(List.of(
                new MacroRow("Protein", plan.proteinG() + " g", plan.proteinPercent() + "%"),
                new MacroRow("Carbohydrate", plan.carbG() + " g", plan.carbPercent() + "%"),
                new MacroRow("Fat", plan.fatG() + " g", plan.fatPercent() + "%"),
                new MacroRow("Nitrogen", UiFormat.number(plan.nitrogenG()) + " g", "")));
        if (plan.fiberApplicable()) {
            rows.add(new MacroRow("Fibre", UiFormat.number(plan.fiberG()) + " g", ""));
        }
        Grid<MacroRow> grid = new Grid<>();
        grid.addColumn(MacroRow::nutrient).setHeader("Nutrient").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(MacroRow::amount).setHeader("Amount / 24 h")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        grid.addColumn(MacroRow::share).setHeader("% kcal")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        grid.setItems(rows);
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setWidth("28em");
        return grid;
    }

    /** Electrolytes delivered over 24 h, grams. */
    private static Grid<ElectrolyteRow> electrolyteGrid(NutritionRegimen.Electrolytes el) {
        Grid<ElectrolyteRow> grid = new Grid<>();
        grid.addColumn(ElectrolyteRow::name).setHeader("Electrolyte").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(ElectrolyteRow::amount).setHeader("g / 24 h")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        grid.setItems(
                new ElectrolyteRow("Sodium (Na)", UiFormat.number(el.sodiumG())),
                new ElectrolyteRow("Potassium (K)", UiFormat.number(el.potassiumG())),
                new ElectrolyteRow("Chloride (Cl)", UiFormat.number(el.chlorideG())),
                new ElectrolyteRow("Calcium (Ca)", UiFormat.number(el.calciumG())),
                new ElectrolyteRow("Magnesium (Mg)", UiFormat.number(el.magnesiumG())),
                new ElectrolyteRow("Phosphorus (P)", UiFormat.number(el.phosphorusG())));
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setWidth("22em");
        return grid;
    }

    private static Span muted(String text) {
        Span span = new Span(text);
        span.getStyle().set("color", "var(--lumo-secondary-text-color)").set("white-space", "normal");
        return span;
    }

    /** A bold sub-heading for a block within the regimen output. */
    private static Span sectionLabel(String text) {
        Span span = new Span(text);
        span.getStyle().set("font-weight", "600").set("display", "block")
                .set("margin-bottom", "var(--lumo-space-xs)");
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
}
