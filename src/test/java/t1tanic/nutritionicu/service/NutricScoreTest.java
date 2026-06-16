package t1tanic.nutritionicu.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import t1tanic.nutritionicu.dto.NutricScore;
import t1tanic.nutritionicu.model.enums.AdmissionDelayBand;
import t1tanic.nutritionicu.model.enums.AgeBand;
import t1tanic.nutritionicu.model.enums.ApacheBand;
import t1tanic.nutritionicu.model.enums.ComorbidityBand;
import t1tanic.nutritionicu.model.enums.Il6Band;
import t1tanic.nutritionicu.model.enums.SofaBand;

/** Verifies NUTRIC scoring and the low/high-risk thresholds (repos unused here). */
class NutricScoreTest {

    private final NutritionService service = new NutritionServiceImpl(null, null, null);

    @Test
    void lowRiskWhenAllBenign() {
        NutricScore s = service.computeNutric(AgeBand.LT_50, ApacheBand.LT_15, SofaBand.LT_6,
                ComorbidityBand.LE_1, AdmissionDelayBand.LT_1, null);
        assertThat(s.score()).isZero();
        assertThat(s.maxScore()).isEqualTo(9);
        assertThat(s.highRisk()).isFalse();
        assertThat(s.includesIl6()).isFalse();
    }

    @Test
    void maxesOutWithoutIl6() {
        NutricScore s = service.computeNutric(AgeBand.GE_75, ApacheBand.GE_28, SofaBand.GE_10,
                ComorbidityBand.GE_2, AdmissionDelayBand.GE_1, null);
        assertThat(s.score()).isEqualTo(9);
        assertThat(s.highRisk()).isTrue();
    }

    @Test
    void thresholdIsFiveWithoutIl6() {
        // 1 + 1 + 1 + 1 + 1 = 5 → high
        NutricScore s = service.computeNutric(AgeBand.B50_74, ApacheBand.B15_19, SofaBand.B6_9,
                ComorbidityBand.GE_2, AdmissionDelayBand.GE_1, null);
        assertThat(s.score()).isEqualTo(5);
        assertThat(s.highRisk()).isTrue();
    }

    @Test
    void il6VariantUsesTenPointScaleAndSixThreshold() {
        NutricScore withHigh = service.computeNutric(AgeBand.B50_74, ApacheBand.B15_19, SofaBand.B6_9,
                ComorbidityBand.GE_2, AdmissionDelayBand.GE_1, Il6Band.GE_400);
        assertThat(withHigh.maxScore()).isEqualTo(10);
        assertThat(withHigh.score()).isEqualTo(6);
        assertThat(withHigh.highRisk()).isTrue();

        NutricScore withLow = service.computeNutric(AgeBand.B50_74, ApacheBand.B15_19, SofaBand.B6_9,
                ComorbidityBand.GE_2, AdmissionDelayBand.GE_1, Il6Band.LT_400);
        assertThat(withLow.score()).isEqualTo(5);
        assertThat(withLow.highRisk()).isFalse(); // 5 < 6 with IL-6
    }
}
