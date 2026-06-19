package t1tanic.nutritionicu.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.LocalDate;
import t1tanic.nutritionicu.dto.EnergyExpenditureResult;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.enums.Sex;
import t1tanic.nutritionicu.model.enums.StressFactor;
import t1tanic.nutritionicu.repo.PatientRepository;
import t1tanic.nutritionicu.service.HarrisBenedictCalculator;

/**
 * Harris-Benedict energy-expenditure screen: pick a patient to seed sex/age/height/weight from their
 * anthropometry, choose a stress degree, and see basal (GEB) and total (GET) energy expenditure.
 * The inputs stay editable for what-if calculations. Reproduces the rccc.eu calculator.
 */
@Route(value = "energy", layout = MainLayout.class)
@PageTitle("Energy expenditure · ICU Nutrition")
public class EnergyExpenditureView extends VerticalLayout {

    private final PatientRepository patientRepository;
    private final HarrisBenedictCalculator calculator;

    private final ComboBox<Patient> patientBox = new ComboBox<>("Patient");
    private final ComboBox<Sex> sexBox = new ComboBox<>("Sex");
    private final NumberField ageField = new NumberField("Age (years)");
    private final NumberField heightField = new NumberField("Height (cm)");
    private final NumberField weightField = new NumberField("Weight (kg)");
    private final ComboBox<StressFactor> stressBox = new ComboBox<>("Stress degree");
    private final Div results = new Div();

    public EnergyExpenditureView(PatientRepository patientRepository,
                                 HarrisBenedictCalculator calculator) {
        this.patientRepository = patientRepository;
        this.calculator = calculator;
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

        Span note = new Span("Decision support only. Reproduces the rccc.eu Harris-Benedict "
                + "calculator: total = basal × (0.1 activity + stress factor), with an obesity weight "
                + "adjustment above BMI 30.");
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
            return;
        }

        EnergyExpenditureResult r = calculator.calculate(
                sex, weight, height, age.intValue(), stress);

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
