package t1tanic.nutritionicu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import t1tanic.nutritionicu.dto.EnergyExpenditureResult;
import t1tanic.nutritionicu.model.enums.Sex;
import t1tanic.nutritionicu.model.enums.StressFactor;
import t1tanic.nutritionicu.model.enums.WeightClass;

/** Verifies the calculator against the rccc.eu Harris-Benedict reference, value for value. */
class HarrisBenedictCalculatorTest {

    private final HarrisBenedictCalculator calculator = new HarrisBenedictCalculator();

    @Test
    void normalWeightMaleMatchesReference() {
        // GEB = 66.5 + 13.75·70 + 5.003·175 − 6.775·40 = 1633.5 → 1634; GET = ·(0.1+1.0) = 1797.
        EnergyExpenditureResult r =
                calculator.calculate(Sex.MALE, 70, 175, 40, StressFactor.NO_STRESS);
        assertThat(r.weightClass()).isEqualTo(WeightClass.NORMAL);
        assertThat(r.bmi()).isEqualTo(22.9);
        assertThat(r.weightUsedKg()).isEqualTo(70.0);
        assertThat(r.basalKcalPerDay()).isEqualTo(1634);
        assertThat(r.totalKcalPerDay()).isEqualTo(1797);
        assertThat(r.kcalPerKgPerDay()).isEqualTo(25.7);
    }

    @Test
    void normalWeightFemaleMatchesReference() {
        // GEB = 655.1 + 9.563·60 + 1.85·165 − 4.676·50 = 1300.3 → 1300; GET = ·1.1 = 1430.
        EnergyExpenditureResult r =
                calculator.calculate(Sex.FEMALE, 60, 165, 50, StressFactor.NO_STRESS);
        assertThat(r.weightClass()).isEqualTo(WeightClass.NORMAL);
        assertThat(r.basalKcalPerDay()).isEqualTo(1300);
        assertThat(r.totalKcalPerDay()).isEqualTo(1430);
    }

    @Test
    void stressFactorScalesTotalAroundTheActivityBaseline() {
        // Same basal (1634), GET = 1634 × (0.1 + 1.45) = 1634 × 1.55 = 2532.7 → 2533.
        EnergyExpenditureResult r =
                calculator.calculate(Sex.MALE, 70, 175, 40, StressFactor.SEVERE_SEPSIS);
        assertThat(r.basalKcalPerDay()).isEqualTo(1634);
        assertThat(r.totalKcalPerDay()).isEqualTo(2533);
    }

    @Test
    void obesityFeedsAdjustedWeightNotActualWeight() {
        // BMI 35.9 → obese band uses adjusted weight (≈80 kg) instead of the 110 kg actual,
        // so basal is far below what the raw weight would give.
        EnergyExpenditureResult r =
                calculator.calculate(Sex.MALE, 110, 175, 40, StressFactor.NO_STRESS);
        assertThat(r.weightClass()).isEqualTo(WeightClass.OBESE);
        assertThat(r.weightUsedKg()).isEqualTo(80.0);
        assertThat(r.basalKcalPerDay()).isEqualTo(1771);
        assertThat(r.weightUsedKg()).isLessThan(110.0);
    }

    @Test
    void morbidObesityUsesTheStrongerWeightCorrection() {
        // BMI > 40 → 50% correction pulls the weight harder toward ideal than the 25% band would.
        EnergyExpenditureResult obese =
                calculator.calculate(Sex.MALE, 110, 175, 40, StressFactor.NO_STRESS);
        EnergyExpenditureResult morbid =
                calculator.calculate(Sex.MALE, 140, 175, 40, StressFactor.NO_STRESS);
        assertThat(morbid.weightClass()).isEqualTo(WeightClass.MORBIDLY_OBESE);
        // 50% of (140 − ideal) still lands below the actual 140 kg.
        assertThat(morbid.weightUsedKg()).isLessThan(140.0);
        assertThat(morbid.weightUsedKg()).isGreaterThan(obese.weightUsedKg());
    }

    @Test
    void rejectsUnknownSexAndNonPositiveInputs() {
        assertThatThrownBy(() -> calculator.calculate(Sex.UNKNOWN, 70, 175, 40, StressFactor.NO_STRESS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculate(Sex.MALE, 0, 175, 40, StressFactor.NO_STRESS))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
