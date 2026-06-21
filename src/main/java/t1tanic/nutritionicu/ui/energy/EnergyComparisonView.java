package t1tanic.nutritionicu.ui.energy;
import t1tanic.nutritionicu.ui.common.TrendChart;
import t1tanic.nutritionicu.ui.common.UiFormat;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import t1tanic.nutritionicu.model.EnergyAssessment;
import t1tanic.nutritionicu.model.enums.EnergyMethod;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.service.nutrition.EnergyAssessmentService;
import t1tanic.nutritionicu.service.patient.PatientService;

/**
 * Overlays a patient's Harris-Benedict predictions and indirect-calorimetry measurements (both kcal/day)
 * on one chart, with a "measured vs predicted" readout — the core measured-vs-predicted comparison.
 * Hosted as a tab inside {@link EnergyView}.
 */
public class EnergyComparisonView extends VerticalLayout {

    private final transient PatientService patientService;
    private final transient EnergyAssessmentService energyService;

    private final ComboBox<Patient> patientBox = new ComboBox<>("Patient");
    private final Span prompt = new Span("Select a patient to compare Harris-Benedict vs calorimetry.");
    private final Div summaryHolder = new Div();
    private final Div chartHolder = new Div();

    private Patient selectedPatient;

    public EnergyComparisonView(PatientService patientService, EnergyAssessmentService energyService) {
        this.patientService = patientService;
        this.energyService = energyService;
        setWidthFull();
        setPadding(false);

        patientBox.setItems(patientService.findAll());
        patientBox.setItemLabelGenerator(p -> p.getFullName() + " (" + p.getMedicalRecordNumber() + ")");
        patientBox.setWidth("22em");
        patientBox.addValueChangeListener(e -> {
            selectedPatient = e.getValue();
            render();
        });

        chartHolder.setWidthFull();
        summaryHolder.getStyle().set("margin", "var(--lumo-space-s) 0");
        add(patientBox, prompt, summaryHolder, chartHolder);
        render();
    }

    private void render() {
        summaryHolder.removeAll();
        chartHolder.removeAll();
        Patient patient = selectedPatient == null
                ? null
                : patientService.findById(selectedPatient.getId()).orElse(null);
        prompt.setVisible(patient == null);
        if (patient == null) {
            return;
        }

        List<EnergyAssessment> hb = energyService.history(patient.getId(), EnergyMethod.HARRIS_BENEDICT);
        List<EnergyAssessment> ic = energyService.history(patient.getId(), EnergyMethod.INDIRECT_CALORIMETRY);
        if (hb.isEmpty() && ic.isEmpty()) {
            Span none = new Span("No energy assessments recorded for this patient yet.");
            none.addClassName(LumoUtility.TextColor.SECONDARY);
            summaryHolder.add(none);
            return;
        }

        summaryHolder.add(summary(
                energyService.latest(patient.getId(), EnergyMethod.INDIRECT_CALORIMETRY),
                energyService.latest(patient.getId(), EnergyMethod.HARRIS_BENEDICT)));

        List<TrendChart.Series> series = new ArrayList<>();
        if (!hb.isEmpty()) {
            series.add(new TrendChart.Series("Harris-Benedict (predicted)", pointsOf(hb)));
        }
        if (!ic.isEmpty()) {
            series.add(new TrendChart.Series("Indirect calorimetry (measured)", pointsOf(ic)));
        }
        chartHolder.add(new TrendChart(series, "kcal/day"));
    }

    private Component summary(Optional<EnergyAssessment> ic, Optional<EnergyAssessment> hb) {
        VerticalLayout box = new VerticalLayout();
        box.setPadding(false);
        box.setSpacing(false);
        box.getStyle().set("gap", "var(--lumo-space-xs)");
        box.add(kv("Latest measured (IC)", ic.map(EnergyComparisonView::valueText).orElse(UiFormat.EMPTY)));
        box.add(kv("Latest predicted (HB)", hb.map(EnergyComparisonView::valueText).orElse(UiFormat.EMPTY)));
        if (ic.isPresent() && hb.isPresent()) {
            int measured = ic.get().getTotalKcalPerDay();
            int predicted = hb.get().getTotalKcalPerDay();
            double pct = measured * 100.0 / predicted;
            int diff = measured - predicted;
            Span label = new Span("Measured vs predicted");
            label.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            label.setWidth("13em");
            HorizontalLayout row = new HorizontalLayout(
                    label, pctPill(pct), new Span("(%+d kcal/day)".formatted(diff)));
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            box.add(row);
        }
        return box;
    }

    private static HorizontalLayout kv(String label, String value) {
        Span name = new Span(label);
        name.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        name.setWidth("13em");
        HorizontalLayout row = new HorizontalLayout(name, new Span(value));
        row.setAlignItems(FlexComponent.Alignment.BASELINE);
        return row;
    }

    private static String valueText(EnergyAssessment a) {
        return a.getTotalKcalPerDay() + " kcal/day · " + UiFormat.date(a.getAssessedOn());
    }

    /** "N% of predicted" pill, green near 100%, orange for a moderate gap, red for a large one. */
    private static Span pctPill(double pct) {
        double deviation = Math.abs(pct - 100);
        String bg;
        String fg;
        if (deviation <= 10) {
            bg = "#E6F4EA";
            fg = "#2E7D32";
        } else if (deviation <= 25) {
            bg = "#FFEBD6";
            fg = "#E65100";
        } else {
            bg = "#FCE4E4";
            fg = "#C62828";
        }
        Span pill = new Span(Math.round(pct) + "% of predicted");
        pill.getStyle().set("background-color", bg).set("color", fg)
                .set("padding", "0.1em 0.6em").set("border-radius", "var(--lumo-border-radius-s)")
                .set("font-weight", "500").set("white-space", "nowrap");
        return pill;
    }

    private static List<TrendChart.Point> pointsOf(List<EnergyAssessment> assessments) {
        return assessments.stream()
                .map(a -> new TrendChart.Point(
                        a.getAssessedOn().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        (double) a.getTotalKcalPerDay()))
                .toList();
    }
}
