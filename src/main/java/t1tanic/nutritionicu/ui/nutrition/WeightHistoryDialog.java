package t1tanic.nutritionicu.ui.nutrition;
import t1tanic.nutritionicu.ui.common.TrendChart;
import t1tanic.nutritionicu.ui.common.UiFormat;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.WeightMeasurement;
import t1tanic.nutritionicu.service.nutrition.NutritionService;

/**
 * Lets a doctor add or edit a patient's weight for a given date and see the weight
 * trend. Entering a date that already exists updates that day's value.
 */
public class WeightHistoryDialog extends Dialog {

    private final transient NutritionService nutritionService;
    private final Long patientId;

    private final DatePicker date = new DatePicker("Date");
    private final NumberField weight = new NumberField("Weight (kg)");
    private final Div chartHolder = new Div();
    private final Grid<WeightMeasurement> grid = new Grid<>(WeightMeasurement.class, false);

    public WeightHistoryDialog(Patient patient, NutritionService nutritionService) {
        this.nutritionService = nutritionService;
        this.patientId = patient.getId();
        setHeaderTitle("Weight history · " + patient.getFullName());
        setWidth("680px");

        UiFormat.dayMonthYear(date);
        date.setValue(LocalDate.now());
        date.setMax(LocalDate.now());
        Button addOrUpdate = new Button("Add / update", e -> save());
        addOrUpdate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout form = new HorizontalLayout(date, weight, addOrUpdate);
        form.setAlignItems(FlexComponent.Alignment.BASELINE);

        chartHolder.setWidthFull();

        grid.addColumn(m -> UiFormat.date(m.getMeasuredOn())).setHeader("Date").setAutoWidth(true);
        grid.addColumn(WeightMeasurement::getWeightKg).setHeader("Weight (kg)").setAutoWidth(true);
        grid.addComponentColumn(m -> new Button("Delete", e -> {
            nutritionService.deleteWeight(m.getId());
            refresh();
        })).setHeader("").setAutoWidth(true);
        grid.setAllRowsVisible(true);

        add(form, chartHolder, grid);
        getFooter().add(new Button("Close", e -> close()));
        refresh();
    }

    private void save() {
        if (date.getValue() == null || weight.getValue() == null) {
            return;
        }
        nutritionService.recordWeight(patientId, date.getValue(), weight.getValue());
        weight.clear();
        refresh();
    }

    private void refresh() {
        List<WeightMeasurement> history = nutritionService.weightHistory(patientId);
        grid.setItems(history);

        List<TrendChart.Point> points = history.stream()
                .filter(m -> m.getWeightKg() != null)
                .map(m -> new TrendChart.Point(
                        m.getMeasuredOn().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        m.getWeightKg()))
                .toList();
        chartHolder.removeAll();
        chartHolder.add(new TrendChart(points, null, null, "kg"));
    }
}
