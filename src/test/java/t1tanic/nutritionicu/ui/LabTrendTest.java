package t1tanic.nutritionicu.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import t1tanic.nutritionicu.model.LabResult;

/** Verifies the trend/range math of {@link LabTrend} in isolation from the UI. */
class LabTrendTest {

    @Test
    void emptyTrendHasNoDataOrTrend() {
        LabTrend trend = new LabTrend(List.of());
        assertThat(trend.hasData()).isFalse();
        assertThat(trend.hasTrend()).isFalse();
        assertThat(trend.direction()).isZero();
        assertThat(trend.latestText()).isEqualTo(UiFormat.EMPTY);
    }

    @Test
    void singleReadingHasDataButNoTrend() {
        LabTrend trend = new LabTrend(List.of(reading(5.0, 0.0, 10.0, "mg/L")));
        assertThat(trend.hasData()).isTrue();
        assertThat(trend.hasTrend()).isFalse();
        assertThat(trend.direction()).isZero();
    }

    @Test
    void elevatedAndLowReadAgainstTheReferenceRange() {
        assertThat(new LabTrend(List.of(reading(12.0, 0.0, 10.0, "mg/L"))).elevated()).isTrue();
        assertThat(new LabTrend(List.of(reading(8.0, 0.0, 10.0, "mg/L"))).elevated()).isFalse();
        assertThat(new LabTrend(List.of(reading(-1.0, 0.0, 10.0, "mg/L"))).low()).isTrue();
        assertThat(new LabTrend(List.of(reading(5.0, 0.0, 10.0, "mg/L"))).low()).isFalse();
    }

    @Test
    void directionRisesAndFallsOnlyBeyondTheFivePercentDeadband() {
        // +20% over 100 -> rising
        LabTrend rising = new LabTrend(List.of(noRef(100.0), noRef(120.0)));
        assertThat(rising.rising()).isTrue();
        assertThat(rising.direction()).isEqualTo(1);

        // -20% -> falling
        LabTrend falling = new LabTrend(List.of(noRef(100.0), noRef(80.0)));
        assertThat(falling.falling()).isTrue();
        assertThat(falling.direction()).isEqualTo(-1);

        // +3% is within the 5% deadband -> flat
        LabTrend flat = new LabTrend(List.of(noRef(100.0), noRef(103.0)));
        assertThat(flat.direction()).isZero();
        assertThat(flat.rising()).isFalse();
        assertThat(flat.falling()).isFalse();
    }

    @Test
    void latestTextAndRefTextFormatLatestValueAndRange() {
        LabTrend trend = new LabTrend(List.of(reading(5.0, 0.0, 10.0, "mg/L"), reading(7.5, 0.0, 10.0, "mg/L")));
        assertThat(trend.latestText()).isEqualTo("7.5 mg/L");
        assertThat(trend.refText()).isEqualTo("ref 0.0–10.0");
    }

    @Test
    void refTextHandlesOpenEndedAndMissingRanges() {
        assertThat(new LabTrend(List.of(reading(5.0, null, 10.0, "mg/L"))).refText())
                .isEqualTo("ref ≤ 10.0");
        assertThat(new LabTrend(List.of(reading(5.0, 2.0, null, "mg/L"))).refText())
                .isEqualTo("ref ≥ 2.0");
        assertThat(new LabTrend(List.of(noRef(5.0))).refText()).isEqualTo("no ref");
    }

    @Test
    void unitAndRangePickTheLatestNonNullValue() {
        LabTrend trend = new LabTrend(List.of(
                reading(5.0, 0.0, 10.0, "mg/L"),
                reading(6.0, null, null, null)));
        assertThat(trend.unit()).isEqualTo("mg/L");
        assertThat(trend.refLow()).isEqualTo(0.0);
        assertThat(trend.refHigh()).isEqualTo(10.0);
    }

    private static LabResult reading(double value, Double refLow, Double refHigh, String unit) {
        LabResult r = new LabResult();
        r.setValueNumeric(BigDecimal.valueOf(value));
        r.setObservedAt(LocalDateTime.now());
        if (refLow != null) {
            r.setRefLow(BigDecimal.valueOf(refLow));
        }
        if (refHigh != null) {
            r.setRefHigh(BigDecimal.valueOf(refHigh));
        }
        r.setUnitRaw(unit);
        return r;
    }

    private static LabResult noRef(double value) {
        return reading(value, null, null, null);
    }
}
