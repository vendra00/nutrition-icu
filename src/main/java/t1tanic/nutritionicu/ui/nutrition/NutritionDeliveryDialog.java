package t1tanic.nutritionicu.ui.nutrition;
import t1tanic.nutritionicu.ui.common.I18n;
import t1tanic.nutritionicu.ui.common.TrendChart;
import t1tanic.nutritionicu.ui.common.UiFormat;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import t1tanic.nutritionicu.model.NutritionDelivery;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.security.SecurityUtils;
import t1tanic.nutritionicu.service.nutrition.NutritionDeliveryService;

/**
 * Lets a doctor record what a patient actually received vs. what was prescribed for their feed
 * (prescribed and actual infusion rate, ml/h), and see the delivery-adequacy trend (% delivered).
 * Entering a date that already exists updates that day's value.
 */
public class NutritionDeliveryDialog extends Dialog {

    private final transient NutritionDeliveryService deliveryService;
    private final Long patientId;

    private final DatePicker date = new DatePicker(I18n.t("nd.date"));
    private final NumberField prescribed = new NumberField(I18n.t("nd.delivery.prescribed"));
    private final NumberField actual = new NumberField(I18n.t("nd.delivery.actual"));
    private final NumberField density = new NumberField(I18n.t("nd.delivery.density"));
    private final Div chartHolder = new Div();
    private final Grid<NutritionDelivery> grid = new Grid<>(NutritionDelivery.class, false);

    public NutritionDeliveryDialog(Patient patient, NutritionDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
        this.patientId = patient.getId();
        setHeaderTitle(getTranslation("nd.delivery.title", patient.getFullName()));
        setWidth("760px");

        UiFormat.dayMonthYear(date);
        date.setValue(LocalDate.now());
        date.setMax(LocalDate.now());
        prescribed.setMin(0);
        actual.setMin(0);
        density.setMin(0);
        density.setStep(0.1);
        density.setHelperText(getTranslation("nd.delivery.densityhelper"));
        Button addOrUpdate = new Button(getTranslation("nd.addupdate"), e -> save());
        addOrUpdate.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout form = new HorizontalLayout(date, prescribed, actual, density, addOrUpdate);
        form.setAlignItems(FlexComponent.Alignment.BASELINE);

        chartHolder.setWidthFull();

        grid.addColumn(d -> UiFormat.date(d.getMeasuredOn()))
                .setHeader(getTranslation("nd.date")).setAutoWidth(true);
        grid.addColumn(d -> UiFormat.number(d.getPrescribedMlPerHour()) + " ml/h")
                .setHeader(getTranslation("nd.delivery.col.prescribed")).setAutoWidth(true);
        grid.addColumn(d -> UiFormat.number(d.getActualMlPerHour()) + " ml/h")
                .setHeader(getTranslation("nd.delivery.col.actual")).setAutoWidth(true);
        grid.addColumn(NutritionDeliveryDialog::deliveredKcal)
                .setHeader(getTranslation("nd.delivery.col.delivered")).setAutoWidth(true);
        grid.addComponentColumn(d -> pctPill(d.percentDelivered()))
                .setHeader(getTranslation("nd.delivery.col.pct")).setAutoWidth(true);
        if (SecurityUtils.isAdmin()) {
            grid.addComponentColumn(d -> new Button(getTranslation("common.delete"), e -> {
                deliveryService.delete(d.getId());
                refresh();
            })).setHeader("").setAutoWidth(true);
        }
        grid.setAllRowsVisible(true);

        add(form, chartHolder, grid);
        getFooter().add(new Button(getTranslation("common.close"), e -> close()));
        refresh();
    }

    private void save() {
        if (date.getValue() == null || prescribed.getValue() == null || actual.getValue() == null) {
            return;
        }
        deliveryService.record(patientId, date.getValue(),
                prescribed.getValue(), actual.getValue(), density.getValue());
        actual.clear();
        refresh();
    }

    private void refresh() {
        List<NutritionDelivery> history = deliveryService.history(patientId);
        grid.setItems(history);

        List<TrendChart.Point> points = history.stream()
                .filter(d -> d.percentDelivered() != null)
                .map(d -> new TrendChart.Point(
                        d.getMeasuredOn().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        d.percentDelivered()))
                .toList();
        chartHolder.removeAll();
        // Green zone = 80–100% of prescribed (adequate delivery).
        chartHolder.add(new TrendChart(points, 80.0, 100.0, "% delivered"));
    }

    private static String deliveredKcal(NutritionDelivery d) {
        if (d.getKcalPerMl() == null || d.getActualMlPerHour() == null) {
            return UiFormat.EMPTY;
        }
        return Math.round(d.getActualMlPerHour() * 24 * d.getKcalPerMl()) + " " + I18n.t("unit.kcalday");
    }

    /** "% delivered" pill — green ≥90%, orange 70–90% or >110% (under/over), red <70% (marked underfeeding). */
    private static Span pctPill(Double pct) {
        if (pct == null) {
            return new Span(UiFormat.EMPTY);
        }
        String bg;
        String fg;
        String status;
        if (pct < 70) {
            bg = "#FCE4E4";
            fg = "#C62828";
            status = I18n.t("nd.delivery.status.underfeeding");
        } else if (pct < 90) {
            bg = "#FFEBD6";
            fg = "#E65100";
            status = I18n.t("nd.delivery.status.under");
        } else if (pct <= 110) {
            bg = "#E6F4EA";
            fg = "#2E7D32";
            status = I18n.t("nd.delivery.status.adequate");
        } else {
            bg = "#FFEBD6";
            fg = "#E65100";
            status = I18n.t("nd.delivery.status.over");
        }
        Span pill = new Span(Math.round(pct) + "%");
        pill.getElement().setAttribute("title", status);
        pill.getStyle().set("background-color", bg).set("color", fg)
                .set("padding", "0.1em 0.6em").set("border-radius", "var(--lumo-border-radius-s)")
                .set("font-weight", "500").set("white-space", "nowrap").set("cursor", "help");
        return pill;
    }
}
