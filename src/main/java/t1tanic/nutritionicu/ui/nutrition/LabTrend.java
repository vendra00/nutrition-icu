package t1tanic.nutritionicu.ui.nutrition;
import t1tanic.nutritionicu.ui.common.UiFormat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import t1tanic.nutritionicu.model.LabResult;

/**
 * A single marker's numeric readings (oldest first) plus the derived latest value, trend and
 * reference range.
 *
 * <p>Pure read model over {@link LabResult} rows — no UI, no persistence — so the trend heuristics
 * (the 5% deadband in {@link #direction()}, elevated/low against the reference range) can be unit
 * tested on their own.
 */
record LabTrend(List<LabResult> readings) {

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
