package t1tanic.nutritionicu.ui.nutrition;
import t1tanic.nutritionicu.ui.common.TrendChart;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.time.ZoneId;
import java.util.List;
import t1tanic.nutritionicu.model.LabResult;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.service.lab.LabResultService;
import t1tanic.nutritionicu.ui.nutrition.MetabolicInterpreter.Interpretation;

/**
 * Metabolic monitoring panel for the nutrition protocol's "Laboratorio" step.
 *
 * <p>In the critical patient, labs measure tolerance and metabolic response, not nutrition
 * directly. This panel trends the inflammatory markers that place the patient on the
 * Ebb↔Flow axis (CRP, procalcitonin) alongside the short-half-life recovery marker
 * (prealbumin) and albumin for context, and offers a plain-language reading of the phase.
 *
 * <p>View assembly only: the trend math lives in {@link LabTrend} and the clinical reading in
 * {@link MetabolicInterpreter}.
 */
class MetabolicMonitorPanel extends Composite<VerticalLayout> {

    /** Whether a clinically concerning move for a marker is upward (inflammation) or downward (depletion). */
    private enum Concern { HIGH, LOW }

    private record Marker(String code, String label, Concern concern) {
    }

    private static final List<Marker> MARKERS = List.of(
            new Marker("CRP", "C-reactive protein (CRP)", Concern.HIGH),
            new Marker("PROCALCITONIN", "Procalcitonin (PCT)", Concern.HIGH),
            new Marker("PREALBUMIN", "Prealbumin", Concern.LOW),
            new Marker("ALBUMIN", "Albumin", Concern.LOW));

    MetabolicMonitorPanel(Patient patient, LabResultService labResultService) {
        VerticalLayout root = getContent();
        root.setPadding(false);
        root.setSpacing(false);

        MetabolicInterpreter interpreter = new MetabolicInterpreter();
        LabTrend crp = load(labResultService, patient.getId(), MARKERS.get(0));
        LabTrend pct = load(labResultService, patient.getId(), MARKERS.get(1));
        LabTrend prealbumin = load(labResultService, patient.getId(), MARKERS.get(2));

        root.add(badge(interpreter.phase(crp, pct)));
        root.add(badge(interpreter.recovery(crp, prealbumin)));

        boolean any = false;
        for (Marker marker : MARKERS) {
            LabTrend trend = load(labResultService, patient.getId(), marker);
            if (trend.hasData()) {
                root.add(markerBlock(marker, trend));
                any = true;
            }
        }
        if (!any) {
            root.add(new Span("No CRP / procalcitonin / prealbumin / albumin results for this patient."));
        }

        Span note = new Span("Albumin is a negative acute-phase reactant — not a reliable acute "
                + "nutritional marker; shown for context only.");
        note.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");
        root.add(note);
    }

    private Div markerBlock(Marker marker, LabTrend trend) {
        Div block = new Div();
        block.setWidthFull();
        String headline = "%s: %s %s   (%s, %s)".formatted(
                marker.label(), trend.latestText(), trendArrow(trend),
                trend.elevated() ? "above ref" : trend.low() ? "below ref" : "within ref",
                trend.refText());
        H4 title = new H4(headline);
        title.getStyle().set("margin-bottom", "0");
        block.add(title);

        List<TrendChart.Point> points = trend.readings().stream()
                .map(r -> new TrendChart.Point(
                        r.getObservedAt().atZone(ZoneId.systemDefault()).toInstant(),
                        r.getValueNumeric().doubleValue()))
                .toList();
        block.add(new TrendChart(points, trend.refLow(), trend.refHigh(), trend.unit()));
        return block;
    }

    private static Span badge(Interpretation interpretation) {
        Span span = new Span(interpretation.text());
        span.getElement().getThemeList().add("badge " + interpretation.theme());
        span.getStyle().set("margin", "var(--lumo-space-xs) 0").set("white-space", "normal");
        return span;
    }

    private static String trendArrow(LabTrend trend) {
        return switch (trend.direction()) {
            case 1 -> "↑";
            case -1 -> "↓";
            default -> "→";
        };
    }

    private LabTrend load(LabResultService labResultService, Long patientId, Marker marker) {
        List<LabResult> readings = labResultService.seriesByCode(patientId, marker.code()).stream()
                .filter(r -> r.getValueNumeric() != null && r.getObservedAt() != null)
                .toList();
        return new LabTrend(readings);
    }
}
