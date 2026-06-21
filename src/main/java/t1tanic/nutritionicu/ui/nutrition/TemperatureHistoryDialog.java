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
import t1tanic.nutritionicu.model.TemperatureMeasurement;
import t1tanic.nutritionicu.security.SecurityUtils;
import t1tanic.nutritionicu.service.nutrition.NutritionService;

/**
 * Lets a doctor add or edit a patient's body temperature for a given date and see the
 * temperature trend. Entering a date that already exists updates that day's value.
 * Tracking only — temperature does not feed any calculation.
 */
public class TemperatureHistoryDialog extends Dialog {

    private final transient NutritionService nutritionService;
    private final Long patientId;

    private final DatePicker date = new DatePicker("Date");
    private final NumberField temperature = new NumberField("Temperature (°C)");
    private final Div chartHolder = new Div();
    private final Grid<TemperatureMeasurement> grid = new Grid<>(TemperatureMeasurement.class, false);

    public TemperatureHistoryDialog(Patient patient, NutritionService nutritionService) {
        this.nutritionService = nutritionService;
        this.patientId = patient.getId();
        setHeaderTitle("Temperature history · " + patient.getFullName());
        setWidth("680px");

        UiFormat.dayMonthYear(date);
        date.setValue(LocalDate.now());
        date.setMax(LocalDate.now());
        temperature.setStep(0.1);
        temperature.setMin(25);
        temperature.setMax(45);
        Button addOrUpdate = new Button("Add / update", e -> save());
        addOrUpdate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout form = new HorizontalLayout(date, temperature, addOrUpdate);
        form.setAlignItems(FlexComponent.Alignment.BASELINE);

        chartHolder.setWidthFull();

        grid.addColumn(m -> UiFormat.date(m.getMeasuredOn())).setHeader("Date").setAutoWidth(true);
        grid.addColumn(TemperatureMeasurement::getTemperatureCelsius)
                .setHeader("Temperature (°C)").setAutoWidth(true);
        if (SecurityUtils.isAdmin()) {
            grid.addComponentColumn(m -> new Button("Delete", e -> {
                nutritionService.deleteTemperature(m.getId());
                refresh();
            })).setHeader("").setAutoWidth(true);
        }
        grid.setAllRowsVisible(true);

        add(form, chartHolder, grid);
        getFooter().add(new Button("Close", e -> close()));
        refresh();
    }

    private void save() {
        if (date.getValue() == null || temperature.getValue() == null) {
            return;
        }
        nutritionService.recordTemperature(patientId, date.getValue(), temperature.getValue());
        temperature.clear();
        refresh();
    }

    private void refresh() {
        List<TemperatureMeasurement> history = nutritionService.temperatureHistory(patientId);
        grid.setItems(history);

        List<TrendChart.Point> points = history.stream()
                .filter(m -> m.getTemperatureCelsius() != null)
                .map(m -> new TrendChart.Point(
                        m.getMeasuredOn().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        m.getTemperatureCelsius()))
                .toList();
        chartHolder.removeAll();
        chartHolder.add(new TrendChart(points, null, null, "°C"));
    }
}
