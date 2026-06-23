package t1tanic.nutritionicu.ui.nutrition;
import t1tanic.nutritionicu.ui.common.I18n;
import t1tanic.nutritionicu.ui.common.UiFormat;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.NumberField;
import java.util.List;
import t1tanic.nutritionicu.dto.NutritionMetrics;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.service.nutrition.NutritionService;
import t1tanic.nutritionicu.ui.common.MetricsTable;

/**
 * Dialog for the doctor to record a patient's screening anthropometry: height and
 * usual weight. Sex/age are read-only (already known); current weight is tracked
 * separately as dated measurements (see {@link WeightHistoryDialog}). Derived
 * metrics update live, using the patient's latest current weight.
 */
public class PatientAnthropometryEditor extends Dialog {

    private final NumberField height = new NumberField(I18n.t("nd.anthro.height"));
    private final NumberField usualWeight = new NumberField(I18n.t("nd.anthro.usualweight"));
    private final Grid<MetricsTable.Row> metricsGrid = MetricsTable.create(I18n.t("nd.anthro.derived"));

    public PatientAnthropometryEditor(Patient patient,
                                      NutritionService nutritionService,
                                      Runnable onSaved) {
        setHeaderTitle(getTranslation("nd.anthro.title", patient.getFullName()));
        setWidth("420px");

        String sex = patient.getSex() == null ? "" : getTranslation("sex." + patient.getSex().name());
        add(new Span(getTranslation("nd.anthro.summary", sex, UiFormat.ageYears(patient),
                UiFormat.number(patient.getCurrentWeightKg()))));

        setValueIfPresent(height, patient.getHeightCm());
        setValueIfPresent(usualWeight, patient.getUsualWeightKg());
        height.addValueChangeListener(e -> recompute(patient, nutritionService));
        usualWeight.addValueChangeListener(e -> recompute(patient, nutritionService));

        FormLayout form = new FormLayout(height, usualWeight);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        metricsGrid.setWidthFull();

        add(form, metricsGrid);
        recompute(patient, nutritionService);

        Button cancel = new Button(getTranslation("common.cancel"), e -> close());
        Button save = new Button(getTranslation("common.save"), e -> {
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
                new MetricsTable.Row(getTranslation("nd.metric.bmi"), UiFormat.number(m.bmi()), m.bmi()),
                new MetricsTable.Row(getTranslation("nd.metric.ibw"), UiFormat.number(m.idealBodyWeightKg()) + " kg"),
                new MetricsTable.Row(getTranslation("nd.metric.abw"), UiFormat.number(m.adjustedBodyWeightKg()) + " kg"),
                new MetricsTable.Row(getTranslation("nd.metric.weightloss"), UiFormat.number(m.weightLossPercent()) + " %")));
    }

    private static void setValueIfPresent(NumberField field, Double value) {
        if (value != null) {
            field.setValue(value);
        }
    }
}
