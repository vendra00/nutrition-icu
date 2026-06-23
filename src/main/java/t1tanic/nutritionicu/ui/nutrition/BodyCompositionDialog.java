package t1tanic.nutritionicu.ui.nutrition;
import t1tanic.nutritionicu.ui.common.I18n;
import t1tanic.nutritionicu.ui.common.UiFormat;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import java.time.LocalDate;
import t1tanic.nutritionicu.model.BodyCompositionMeasurement;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.security.SecurityUtils;
import t1tanic.nutritionicu.service.nutrition.NutritionService;

/**
 * Records a patient's dated body-composition readings (body fat %, skeletal muscle mass %, bone density,
 * phase angle) and lists the history. Any field may be left blank; entering a date that already exists
 * updates that day's reading. Body composition gives the objective context BMI alone can miss.
 */
public class BodyCompositionDialog extends Dialog {

    private final transient NutritionService nutritionService;
    private final Long patientId;

    private final DatePicker date = new DatePicker(I18n.t("nd.date"));
    private final NumberField fat = field(I18n.t("bodycomp.fat"));
    private final NumberField muscle = field(I18n.t("bodycomp.muscle"));
    private final NumberField bone = field(I18n.t("bodycomp.bone"));
    private final NumberField phase = field(I18n.t("bodycomp.phase"));
    private final Grid<BodyCompositionMeasurement> grid = new Grid<>(BodyCompositionMeasurement.class, false);

    public BodyCompositionDialog(Patient patient, NutritionService nutritionService) {
        this.nutritionService = nutritionService;
        this.patientId = patient.getId();
        setHeaderTitle(getTranslation("bodycomp.title", patient.getFullName()));
        setWidth("820px");

        UiFormat.dayMonthYear(date);
        date.setValue(LocalDate.now());
        date.setMax(LocalDate.now());
        Button addOrUpdate = new Button(getTranslation("nd.addupdate"), e -> save());
        addOrUpdate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout form = new HorizontalLayout(date, fat, muscle, bone, phase, addOrUpdate);
        form.setAlignItems(FlexComponent.Alignment.BASELINE);
        form.getStyle().set("flex-wrap", "wrap");

        grid.addColumn(m -> UiFormat.date(m.getMeasuredOn()))
                .setHeader(getTranslation("nd.date")).setAutoWidth(true);
        grid.addColumn(m -> UiFormat.number(m.getBodyFatPercent()))
                .setHeader(getTranslation("bodycomp.fat")).setAutoWidth(true);
        grid.addColumn(m -> UiFormat.number(m.getSkeletalMusclePercent()))
                .setHeader(getTranslation("bodycomp.muscle")).setAutoWidth(true);
        grid.addColumn(m -> UiFormat.number(m.getBoneDensity()))
                .setHeader(getTranslation("bodycomp.bone")).setAutoWidth(true);
        grid.addColumn(m -> UiFormat.number(m.getPhaseAngle()))
                .setHeader(getTranslation("bodycomp.phase")).setAutoWidth(true);
        if (SecurityUtils.isAdmin()) {
            grid.addComponentColumn(m -> new Button(getTranslation("common.delete"), e -> {
                nutritionService.deleteBodyComposition(m.getId());
                refresh();
            })).setHeader("").setAutoWidth(true);
        }
        grid.setAllRowsVisible(true);

        add(form, grid);
        getFooter().add(new Button(getTranslation("common.close"), e -> close()));
        refresh();
    }

    private void save() {
        if (date.getValue() == null) {
            return;
        }
        // Need at least one measured value; otherwise there's nothing to record.
        if (fat.getValue() == null && muscle.getValue() == null
                && bone.getValue() == null && phase.getValue() == null) {
            return;
        }
        nutritionService.recordBodyComposition(patientId, date.getValue(),
                fat.getValue(), muscle.getValue(), bone.getValue(), phase.getValue());
        fat.clear();
        muscle.clear();
        bone.clear();
        phase.clear();
        refresh();
    }

    private void refresh() {
        grid.setItems(nutritionService.bodyCompositionHistory(patientId));
    }

    private static NumberField field(String label) {
        NumberField numberField = new NumberField(label);
        numberField.setMin(0);
        numberField.setWidth("9em");
        return numberField;
    }
}
