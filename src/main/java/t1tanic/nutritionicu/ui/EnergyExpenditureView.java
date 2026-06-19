package t1tanic.nutritionicu.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.LocalDate;
import t1tanic.nutritionicu.dto.EnergyExpenditureResult;
import t1tanic.nutritionicu.dto.NutritionProduct;
import t1tanic.nutritionicu.dto.NutritionRegimen;
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
    private final Div nutritionSection = new Div();
    private final RadioButtonGroup<NutritionCategory> categoryBox = new RadioButtonGroup<>("Type");
    private final ComboBox<NutritionProduct> productBox = new ComboBox<>("Nutrition formula");
    private final Div regimenResults = new Div();

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
        setSizeFull();
        setPadding(true);
        add(new H2("Energy expenditure (Harris-Benedict)"));

        patientBox.setItems(patientRepository.findAll());
        patientBox.setItemLabelGenerator(p -> p.getFullName() + " (" + p.getMedicalRecordNumber() + ")");
        patientBox.addValueChangeListener(e -> prefill(e.getValue()));
        add(patientBox);

        sexBox.setItems(Sex.MALE, Sex.FEMALE);
        sexBox.setItemLabelGenerator(s -> s == Sex.MALE ? "Male" : "Female");
        stressBox.setItems(StressFactor.values());
        stressBox.setItemLabelGenerator(s -> "%s (×%.2f)".formatted(s.label(), s.factor()));
        stressBox.setValue(StressFactor.NO_STRESS);

        FormLayout inputs = new FormLayout(sexBox, ageField, heightField, weightField, stressBox);
        add(inputs);

        sexBox.addValueChangeListener(e -> recompute());
        ageField.addValueChangeListener(e -> recompute());
        heightField.addValueChangeListener(e -> recompute());
        weightField.addValueChangeListener(e -> recompute());
        stressBox.addValueChangeListener(e -> recompute());

        results.getStyle().set("margin-top", "var(--lumo-space-m)");
        add(results);

        categoryBox.setItems(NutritionCategory.values());
        categoryBox.setItemLabelGenerator(NutritionCategory::label);
        categoryBox.setValue(NutritionCategory.ENTERAL);
        categoryBox.addValueChangeListener(e -> applyCategoryFilter());
        productBox.setWidth("28em");
        productBox.setItemLabelGenerator(NutritionProduct::name);
        productBox.addValueChangeListener(e -> renderRegimen());
        applyCategoryFilter();
        // Hidden until a complete energy result exists — the nutrition step comes after the GET.
        nutritionSection.setVisible(false);
        nutritionSection.add(new H3("Choose nutrition"), categoryBox, productBox, regimenResults);
        add(nutritionSection);

        Span note = new Span("Decision support only. Reproduces the rccc.eu Harris-Benedict "
                + "calculator: total = basal × (0.1 activity + stress factor), with an obesity weight "
                + "adjustment above BMI 30. The infusion rate delivers GET over 24 h.");
        note.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");
        add(note);

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
            nutritionSection.setVisible(false);
            renderRegimen();
            return;
        }

        EnergyExpenditureResult r = calculator.calculate(
                sex, weight, height, age.intValue(), stress);
        lastEnergy = r;
        lastActualWeightKg = weight;
        nutritionSection.setVisible(true);

        Span total = new Span("GET (total): %d kcal/day".formatted(r.totalKcalPerDay()));
        total.getElement().getThemeList().add("badge primary");
        total.getStyle().set("font-size", "var(--lumo-font-size-l)").set("white-space", "normal");

        FormLayout out = new FormLayout();
        out.addFormItem(new Span("%d kcal/day".formatted(r.basalKcalPerDay())), "GEB (basal)");
        out.addFormItem(new Span("%s kcal/kg/day".formatted(UiFormat.number(r.kcalPerKgPerDay()))),
                "Per kg (actual weight)");
        out.addFormItem(new Span(UiFormat.number(r.bmi())), "BMI");
        out.addFormItem(new Span(weightBasis(r)), "Weight class");
        out.addFormItem(new Span("%s kg".formatted(UiFormat.number(r.idealBodyWeightKg()))),
                "Ideal body weight");
        out.addFormItem(new Span("%s kg".formatted(UiFormat.number(r.weightUsedKg()))),
                "Weight used in equation");

        results.add(total, out);
        renderRegimen();
    }

    /** Restricts the formula dropdown to the selected category, clearing a now-hidden selection. */
    private void applyCategoryFilter() {
        NutritionCategory category = categoryBox.getValue();
        productBox.clear();
        productBox.setItems(formulary.all().stream()
                .filter(p -> p.category() == category)
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

        Span rate = new Span("Infusion: %d ml/h  (%d ml/day, %s kcal/ml)".formatted(
                plan.infusionMlPerHour(), plan.dailyVolumeMl(),
                UiFormat.number(product.densityKcalPerMl())));
        rate.getElement().getThemeList().add("badge primary");
        rate.getStyle().set("font-size", "var(--lumo-font-size-l)").set("white-space", "normal");

        FormLayout macros = new FormLayout();
        macros.addFormItem(new Span("%d g  (%d%%)".formatted(plan.proteinG(), plan.proteinPercent())),
                "Protein");
        macros.addFormItem(new Span("%d g  (%d%%)".formatted(plan.carbG(), plan.carbPercent())),
                "Carbohydrate");
        macros.addFormItem(new Span("%d g  (%d%%)".formatted(plan.fatG(), plan.fatPercent())), "Fat");
        macros.addFormItem(new Span("%s g".formatted(UiFormat.number(plan.nitrogenG()))), "Nitrogen");
        if (plan.fiberApplicable()) {
            macros.addFormItem(new Span("%s g".formatted(UiFormat.number(plan.fiberG()))), "Fibre");
        }
        macros.addFormItem(new Span(plan.product().osmolarity() == null
                ? UiFormat.EMPTY : plan.product().osmolarity() + " mOsm/l"), "Osmolarity");

        NutritionRegimen.Electrolytes el = plan.electrolytes();
        FormLayout electrolytes = new FormLayout();
        electrolytes.addFormItem(new Span("%s g".formatted(UiFormat.number(el.sodiumG()))), "Na");
        electrolytes.addFormItem(new Span("%s g".formatted(UiFormat.number(el.potassiumG()))), "K");
        electrolytes.addFormItem(new Span("%s g".formatted(UiFormat.number(el.chlorideG()))), "Cl");
        electrolytes.addFormItem(new Span("%s g".formatted(UiFormat.number(el.calciumG()))), "Ca");
        electrolytes.addFormItem(new Span("%s g".formatted(UiFormat.number(el.magnesiumG()))), "Mg");
        electrolytes.addFormItem(new Span("%s g".formatted(UiFormat.number(el.phosphorusG()))), "P");

        Span proteinTarget = new Span(
                "Protein target: %s g/day  (%s g/kg of %s)".formatted(
                        UiFormat.number(plan.proteinTargetG()),
                        UiFormat.number(plan.proteinTargetPerKg()), plan.proteinBasis()));
        proteinTarget.getStyle().set("white-space", "normal");

        regimenResults.add(rate);
        regimenResults.add(new Span("Delivered over 24 h"), macros);
        regimenResults.add(new Span("Electrolytes (24 h)"), electrolytes);
        regimenResults.add(proteinTarget);
        if (plan.proteinDeficitG() > 0) {
            Span deficit = new Span("Protein deficit vs target: %d g/day".formatted(plan.proteinDeficitG()));
            deficit.getElement().getThemeList().add("badge error");
            deficit.getStyle().set("white-space", "normal");
            regimenResults.add(new Div(deficit));
        }
        if (product.indications() != null && !product.indications().isBlank()) {
            regimenResults.add(muted("Indications: " + product.indications()));
        }
    }

    private static Span muted(String text) {
        Span span = new Span(text);
        span.getStyle().set("color", "var(--lumo-secondary-text-color)").set("white-space", "normal");
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
