package t1tanic.nutritionicu.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import t1tanic.nutritionicu.model.LabResult;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.repo.LabResultRepository;

/**
 * Metabolic monitoring panel for the nutrition protocol's "Laboratorio" step.
 *
 * <p>In the critical patient, labs measure tolerance and metabolic response, not nutrition
 * directly. This panel trends the inflammatory markers that place the patient on the
 * Ebb↔Flow axis (CRP, procalcitonin) alongside the short-half-life recovery marker
 * (prealbumin) and albumin for context, and offers a plain-language reading of the phase.
 *
 * <p>Decision support only — interpretations are heuristic (driven by each marker's own
 * reference range and its recent direction), not a diagnosis.
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

    MetabolicMonitorPanel(Patient patient, LabResultRepository resultRepository) {
        VerticalLayout root = getContent();
        root.setPadding(false);
        root.setSpacing(false);

        Series crp = load(resultRepository, patient.getId(), MARKERS.get(0));
        Series pct = load(resultRepository, patient.getId(), MARKERS.get(1));
        Series prealbumin = load(resultRepository, patient.getId(), MARKERS.get(2));

        root.add(phaseInterpretation(crp, pct));
        root.add(recoveryInterpretation(crp, prealbumin));

        boolean any = false;
        for (Marker marker : MARKERS) {
            Series series = load(resultRepository, patient.getId(), marker);
            if (series.hasData()) {
                root.add(markerBlock(marker, series));
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

    /** Ebb (acute hyperinflammation) vs Flow (catabolism/recovery), read from CRP trend and PCT. */
    private Span phaseInterpretation(Series crp, Series pct) {
        if (!crp.hasData()) {
            return badge("Metabolic phase: insufficient CRP data", "contrast");
        }
        String text;
        String theme;
        if (crp.elevated() && crp.rising()) {
            text = "Ebb phase — acute inflammation rising (CRP %s). Avoid aggressive caloric load."
                    .formatted(crp.latestText());
            theme = "error";
        } else if (crp.elevated() && crp.falling()) {
            text = "Transition to Flow — inflammation elevated but settling (CRP %s)."
                    .formatted(crp.latestText());
            theme = "warning";
        } else if (crp.elevated()) {
            text = "Inflammation elevated (CRP %s); add a follow-up reading to establish the trend."
                    .formatted(crp.latestText());
            theme = "warning";
        } else {
            text = "Flow phase — low inflammation (CRP %s); anabolic window."
                    .formatted(crp.latestText());
            theme = "success";
        }
        if (pct.hasData() && pct.elevated()) {
            text += " Procalcitonin elevated (%s) — consider ongoing sepsis.".formatted(pct.latestText());
            theme = "error";
        }
        return badge("Metabolic phase: " + text, theme);
    }

    /** Prealbumin only reads as a nutrition trend once the acute-phase response is settling. */
    private Span recoveryInterpretation(Series crp, Series prealbumin) {
        if (!prealbumin.hasData()) {
            return badge("Anabolic recovery: no prealbumin data", "contrast");
        }
        boolean inflammationSettling = !crp.hasData() || !crp.elevated() || crp.falling();
        if (!inflammationSettling) {
            return badge("Anabolic recovery: prealbumin not interpretable while CRP is elevated/rising "
                    + "(acute-phase effect)", "contrast");
        }
        if (!prealbumin.hasTrend()) {
            return badge("Anabolic recovery: single prealbumin reading (%s); needs another to read the trend"
                    .formatted(prealbumin.latestText()), "contrast");
        }
        if (prealbumin.rising()) {
            return badge("Anabolic recovery: prealbumin rising (%s) as inflammation settles — recovery underway"
                    .formatted(prealbumin.latestText()), "success");
        }
        return badge("Anabolic recovery: prealbumin not yet rising (%s) despite settling inflammation"
                .formatted(prealbumin.latestText()), "warning");
    }

    private Div markerBlock(Marker marker, Series series) {
        Div block = new Div();
        block.setWidthFull();
        String headline = "%s: %s %s   (%s, %s)".formatted(
                marker.label(), series.latestText(), trendArrow(series),
                series.elevated() ? "above ref" : series.low() ? "below ref" : "within ref",
                series.refText());
        H4 title = new H4(headline);
        title.getStyle().set("margin-bottom", "0");
        block.add(title);

        List<TrendChart.Point> points = series.readings().stream()
                .map(r -> new TrendChart.Point(
                        r.getObservedAt().atZone(ZoneId.systemDefault()).toInstant(),
                        r.getValueNumeric().doubleValue()))
                .toList();
        block.add(new TrendChart(points, series.refLow(), series.refHigh(), series.unit()));
        return block;
    }

    private static Span badge(String text, String theme) {
        Span span = new Span(text);
        span.getElement().getThemeList().add("badge " + theme);
        span.getStyle().set("margin", "var(--lumo-space-xs) 0").set("white-space", "normal");
        return span;
    }

    private static String trendArrow(Series series) {
        return switch (series.direction()) {
            case 1 -> "↑";
            case -1 -> "↓";
            default -> "→";
        };
    }

    private Series load(LabResultRepository repo, Long patientId, Marker marker) {
        List<LabResult> readings = repo
                .findByPatientIdAndAnalyteCodeOrderByObservedAtAsc(patientId, marker.code()).stream()
                .filter(r -> r.getValueNumeric() != null && r.getObservedAt() != null)
                .toList();
        return new Series(readings);
    }

    /** A marker's numeric readings (oldest first) plus the derived latest value, trend and range. */
    private record Series(List<LabResult> readings) {

        boolean hasData() {
            return !readings.isEmpty();
        }

        boolean hasTrend() {
            return readings.size() >= 2;
        }

        private Double latest() {
            return hasData() ? readings.getLast().getValueNumeric().doubleValue() : null;
        }

        private Double previous() {
            return readings.size() >= 2
                    ? readings.get(readings.size() - 2).getValueNumeric().doubleValue() : null;
        }

        Double refLow() {
            return lastNonNull(LabResult::getRefLow);
        }

        Double refHigh() {
            return lastNonNull(LabResult::getRefHigh);
        }

        String unit() {
            return readings.stream().map(LabResult::getUnitRaw)
                    .filter(Objects::nonNull).reduce((a, b) -> b).orElse(null);
        }

        boolean elevated() {
            Double high = refHigh();
            return high != null && latest() != null && latest() > high;
        }

        boolean low() {
            Double low = refLow();
            return low != null && latest() != null && latest() < low;
        }

        /** +1 rising, -1 falling, 0 flat/unknown, with a 5% deadband to ignore noise. */
        int direction() {
            Double latest = latest();
            Double previous = previous();
            if (latest == null || previous == null) {
                return 0;
            }
            double delta = latest - previous;
            double deadband = Math.abs(previous) * 0.05;
            if (delta > deadband) {
                return 1;
            }
            return delta < -deadband ? -1 : 0;
        }

        boolean rising() {
            return direction() > 0;
        }

        boolean falling() {
            return direction() < 0;
        }

        String latestText() {
            Double latest = latest();
            if (latest == null) {
                return UiFormat.EMPTY;
            }
            String unit = unit();
            return UiFormat.number(latest) + (unit != null ? " " + unit : "");
        }

        String refText() {
            Double low = refLow();
            Double high = refHigh();
            if (low != null && high != null) {
                return "ref " + UiFormat.number(low) + "–" + UiFormat.number(high);
            }
            if (high != null) {
                return "ref ≤ " + UiFormat.number(high);
            }
            if (low != null) {
                return "ref ≥ " + UiFormat.number(low);
            }
            return "no ref";
        }

        private Double lastNonNull(Function<LabResult, BigDecimal> getter) {
            return readings.stream().map(getter).filter(Objects::nonNull)
                    .reduce((a, b) -> b).map(BigDecimal::doubleValue).orElse(null);
        }
    }
}
