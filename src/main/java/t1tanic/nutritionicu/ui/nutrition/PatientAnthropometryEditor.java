package t1tanic.nutritionicu.ui.nutrition;
import t1tanic.nutritionicu.ui.common.UiFormat;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.NumberField;
import java.util.List;
import t1tanic.nutritionicu.dto.NutritionMetrics;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.service.NutritionService;
import t1tanic.nutritionicu.ui.common.BmiBadge;

/**
 * Dialog for the doctor to record a patient's screening anthropometry: height and
 * usual weight. Sex/age are read-only (already known); current weight is tracked
 * separately as dated measurements (see {@link WeightHistoryDialog}). Derived
 * metrics update live, using the patient's latest current weight.
 */
public class PatientAnthropometryEditor extends Dialog {

    private final NumberField height = new NumberField("Height (cm)");
    private final NumberField usualWeight = new NumberField("Usual weight (kg)");
    private final Grid<MetricRow> metricsGrid = new Grid<>();

    public PatientAnthropometryEditor(Patient patient,
                                      NutritionService nutritionService,
                                      Runnable onSaved) {
        setHeaderTitle("Anthropometry · " + patient.getFullName());
        setWidth("420px");

        add(new Span("Sex: " + patient.getSex() + "  ·  Age: " + UiFormat.ageYears(patient)
                + "  ·  Current weight: " + UiFormat.number(patient.getCurrentWeightKg()) + " kg"));

        setValueIfPresent(height, patient.getHeightCm());
        setValueIfPresent(usualWeight, patient.getUsualWeightKg());
        height.addValueChangeListener(e -> recompute(patient, nutritionService));
        usualWeight.addValueChangeListener(e -> recompute(patient, nutritionService));

        FormLayout form = new FormLayout(height, usualWeight);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        metricsGrid.addColumn(MetricRow::metric).setHeader("Derived metric").setAutoWidth(true).setFlexGrow(1);
        metricsGrid.addComponentColumn(MetricRow::valueComponent).setHeader("Value").setAutoWidth(true);
        metricsGrid.setAllRowsVisible(true);
        metricsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        metricsGrid.setWidthFull();

        add(form, metricsGrid);
        recompute(patient, nutritionService);

        Button cancel = new Button("Cancel", e -> close());
        Button save = new Button("Save", e -> {
            nutritionService.updateAnthropometry(patient.getId(), height.getValue(), usualWeight.getValue());
            onSaved.run();
            close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);
    }

    /** Recomputes metrics from the edited height/usual weight and the patient's current weight. */
    private void recompute(Patient patient, NutritionService nutritionService) {
        Patient draft = new Patient();
        draft.setSex(patient.getSex());
        draft.setHeightCm(height.getValue());
        draft.setUsualWeightKg(usualWeight.getValue());
        draft.setCurrentWeightKg(patient.getCurrentWeightKg());

        NutritionMetrics m = nutritionService.metricsFor(draft);
        metricsGrid.setItems(List.of(
                new MetricRow("BMI", UiFormat.number(m.bmi()), m.bmi()),
                new MetricRow("Ideal body weight", UiFormat.number(m.idealBodyWeightKg()) + " kg", null),
                new MetricRow("Adjusted body weight", UiFormat.number(m.adjustedBodyWeightKg()) + " kg", null),
                new MetricRow("Recent weight loss", UiFormat.number(m.weightLossPercent()) + " %", null)));
    }

    private static void setValueIfPresent(NumberField field, Double value) {
        if (value != null) {
            field.setValue(value);
        }
    }

    /** A derived-metric row; {@code bmi} is non-null only for the BMI row, which renders a coloured pill. */
    private record MetricRow(String metric, String value, Double bmi) {

        Span valueComponent() {
            return bmi == null ? new Span(value) : BmiBadge.of(bmi, value);
        }
    }
}
