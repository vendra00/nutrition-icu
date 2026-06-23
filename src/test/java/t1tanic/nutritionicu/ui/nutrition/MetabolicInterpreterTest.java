package t1tanic.nutritionicu.ui.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import t1tanic.nutritionicu.model.LabResult;
import t1tanic.nutritionicu.ui.nutrition.MetabolicInterpreter.Interpretation;

/** Verifies the clinical reading of {@link MetabolicInterpreter} without rendering any UI. */
class MetabolicInterpreterTest {

    private final MetabolicInterpreter interpreter = new MetabolicInterpreter();
    private static final LabTrend NO_DATA = new LabTrend(List.of());

    // The interpreter resolves text through the i18n provider; with no UI bound in tests, getTranslation
    // returns the key itself, so each branch is identified by its distinct translation key.

    @Test
    void phaseReportsInsufficientDataWithoutCrp() {
        Interpretation result = interpreter.phase(NO_DATA, NO_DATA);
        assertThat(result.text()).isEqualTo("metabolic.phase.nodata");
        assertThat(result.theme()).isEqualTo("contrast");
    }

    @Test
    void phaseFlagsEbbWhenCrpElevatedAndRising() {
        LabTrend crp = crp(50.0, 120.0); // rising, above ref 10
        Interpretation result = interpreter.phase(crp, NO_DATA);
        assertThat(result.text()).isEqualTo("metabolic.phase.ebb");
        assertThat(result.theme()).isEqualTo("error");
    }

    @Test
    void phaseFlagsTransitionToFlowWhenElevatedButFalling() {
        LabTrend crp = crp(120.0, 50.0); // falling, still above ref
        Interpretation result = interpreter.phase(crp, NO_DATA);
        assertThat(result.text()).isEqualTo("metabolic.phase.transition");
        assertThat(result.theme()).isEqualTo("warning");
    }

    @Test
    void phaseAsksForFollowUpWhenElevatedWithSingleReading() {
        LabTrend crp = crp(50.0); // elevated, no trend
        Interpretation result = interpreter.phase(crp, NO_DATA);
        assertThat(result.text()).isEqualTo("metabolic.phase.elevated");
        assertThat(result.theme()).isEqualTo("warning");
    }

    @Test
    void phaseFlagsFlowWindowWhenInflammationLow() {
        LabTrend crp = crp(3.0); // below ref 10
        Interpretation result = interpreter.phase(crp, NO_DATA);
        assertThat(result.text()).isEqualTo("metabolic.phase.flow");
        assertThat(result.theme()).isEqualTo("success");
    }

    @Test
    void phaseEscalatesToSepsisConcernWhenProcalcitoninElevated() {
        LabTrend crp = crp(3.0);                 // would be Flow/success on its own
        LabTrend pct = trend(ref(0.0, 0.5), 2.0); // elevated PCT
        Interpretation result = interpreter.phase(crp, pct);
        assertThat(result.text()).contains("metabolic.phase.flow").contains("metabolic.phase.pct");
        assertThat(result.theme()).isEqualTo("error");
    }

    @Test
    void recoveryReportsNoDataWithoutPrealbumin() {
        Interpretation result = interpreter.recovery(NO_DATA, NO_DATA);
        assertThat(result.text()).isEqualTo("metabolic.recovery.nodata");
        assertThat(result.theme()).isEqualTo("contrast");
    }

    @Test
    void recoveryNotInterpretableWhileCrpElevatedAndRising() {
        LabTrend crp = crp(50.0, 120.0); // elevated, rising -> not settling
        LabTrend prealbumin = prealbumin(15.0, 18.0);
        Interpretation result = interpreter.recovery(crp, prealbumin);
        assertThat(result.text()).isEqualTo("metabolic.recovery.notinterpretable");
        assertThat(result.theme()).isEqualTo("contrast");
    }

    @Test
    void recoveryNeedsAnotherReadingWithSinglePrealbumin() {
        Interpretation result = interpreter.recovery(NO_DATA, prealbumin(15.0));
        assertThat(result.text()).isEqualTo("metabolic.recovery.single");
        assertThat(result.theme()).isEqualTo("contrast");
    }

    @Test
    void recoveryUnderwayWhenPrealbuminRisingAsInflammationSettles() {
        LabTrend crp = crp(120.0, 50.0); // settling (falling)
        LabTrend prealbumin = prealbumin(12.0, 18.0); // rising
        Interpretation result = interpreter.recovery(crp, prealbumin);
        assertThat(result.text()).isEqualTo("metabolic.recovery.rising");
        assertThat(result.theme()).isEqualTo("success");
    }

    @Test
    void recoveryNotYetWhenPrealbuminFlatDespiteSettlingInflammation() {
        LabTrend crp = crp(3.0); // low -> settling
        LabTrend prealbumin = prealbumin(15.0, 15.0); // flat
        Interpretation result = interpreter.recovery(crp, prealbumin);
        assertThat(result.text()).isEqualTo("metabolic.recovery.notrising");
        assertThat(result.theme()).isEqualTo("warning");
    }

    /** A CRP trend (reference 0–10) with the given readings, oldest first. */
    private static LabTrend crp(double... values) {
        return trend(ref(0.0, 10.0), values);
    }

    /** A prealbumin trend (reference 20–40, so 12–18 read as low) with the given readings. */
    private static LabTrend prealbumin(double... values) {
        return trend(ref(20.0, 40.0), values);
    }

    private static LabTrend trend(BigDecimal[] refRange, double... values) {
        return new LabTrend(java.util.Arrays.stream(values)
                .mapToObj(v -> reading(v, refRange[0], refRange[1]))
                .toList());
    }

    private static BigDecimal[] ref(double low, double high) {
        return new BigDecimal[] {BigDecimal.valueOf(low), BigDecimal.valueOf(high)};
    }

    private static LabResult reading(double value, BigDecimal refLow, BigDecimal refHigh) {
        LabResult r = new LabResult();
        r.setValueNumeric(BigDecimal.valueOf(value));
        r.setObservedAt(LocalDateTime.now());
        r.setRefLow(refLow);
        r.setRefHigh(refHigh);
        return r;
    }
}
