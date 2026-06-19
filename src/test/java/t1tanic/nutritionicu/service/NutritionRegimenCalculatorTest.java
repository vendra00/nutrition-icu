package t1tanic.nutritionicu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;
import t1tanic.nutritionicu.dto.EnergyExpenditureResult;
import t1tanic.nutritionicu.dto.NutritionRegimen;
import t1tanic.nutritionicu.model.NutritionProduct;
import t1tanic.nutritionicu.model.enums.NutritionCategory;
import t1tanic.nutritionicu.model.enums.StressFactor;
import t1tanic.nutritionicu.model.enums.WeightClass;

/** Verifies the 24-hour administration plan against the rccc.eu {@code cX} routine. */
class NutritionRegimenCalculatorTest {

    private final NutritionRegimenCalculator calculator = new NutritionRegimenCalculator();

    // Nutrison®: 1 kcal/ml, protein 4 / carb 12.3 / fat 3.9 / fibre 0.1 g per 100 ml.
    private static final NutritionProduct NUTRISON = new NutritionProduct(
            "NS", NutritionCategory.ENTERAL, "Nutrison®",
            1.0, 4, 12.3, 3.9, 0.1, "255", 100, 150, 125, 23, 80, 72, "Normocalórica");

    private static EnergyExpenditureResult energy(int get, double bmi, double idealKg) {
        return new EnergyExpenditureResult(bmi, WeightClass.NORMAL, idealKg, 70, 1800, get, 28.6,
                StressFactor.NO_STRESS);
    }

    @Test
    void infusionRateDeliversGetOver24Hours() {
        // GET 2000 / (1 kcal/ml × 24 h) = 83.3 → 83 ml/h → 1992 ml/day.
        NutritionRegimen plan = calculator.calculate(energy(2000, 22, 65), 70, NUTRISON);
        assertThat(plan.infusionMlPerHour()).isEqualTo(83);
        assertThat(plan.dailyVolumeMl()).isEqualTo(1992);
        assertThat(plan.totalKcal()).isEqualTo(2000);
    }

    @Test
    void scalesMacrosAndPercentagesFromFormulaComposition() {
        NutritionRegimen plan = calculator.calculate(energy(2000, 22, 65), 70, NUTRISON);
        assertThat(plan.proteinG()).isEqualTo(80);
        assertThat(plan.proteinPercent()).isEqualTo(16);
        assertThat(plan.carbG()).isEqualTo(245);
        assertThat(plan.carbPercent()).isEqualTo(50);
        assertThat(plan.fatG()).isEqualTo(78);
        assertThat(plan.fatPercent()).isEqualTo(35);
        assertThat(plan.nitrogenG()).isCloseTo(12.75, within(1e-9));
        assertThat(plan.fiberG()).isCloseTo(1.99, within(1e-9));
        assertThat(plan.fiberApplicable()).isTrue();
    }

    @Test
    void scalesElectrolytesToGramsPerDay() {
        NutritionRegimen.Electrolytes el = calculator.calculate(energy(2000, 22, 65), 70, NUTRISON)
                .electrolytes();
        assertThat(el.sodiumG()).isCloseTo(2.0, within(1e-9));
        assertThat(el.potassiumG()).isCloseTo(3.0, within(1e-9));
        assertThat(el.chlorideG()).isCloseTo(2.5, within(1e-9));
        assertThat(el.calciumG()).isCloseTo(1.6, within(1e-9));
        assertThat(el.magnesiumG()).isCloseTo(0.5, within(1e-9));
        assertThat(el.phosphorusG()).isCloseTo(1.4, within(1e-9));
    }

    @Test
    void proteinTargetForNormalBmiUsesActualWeightAtOnePointFive() {
        NutritionRegimen plan = calculator.calculate(energy(2000, 22, 65), 70, NUTRISON);
        assertThat(plan.proteinTargetG()).isCloseTo(105.0, within(1e-9)); // 70 × 1.5
        assertThat(plan.proteinTargetPerKg()).isCloseTo(1.5, within(1e-9));
        assertThat(plan.proteinBasis()).isEqualTo("actual weight");
        assertThat(plan.proteinDeficitG()).isEqualTo(25); // 105 target − 80 delivered
    }

    @Test
    void proteinTargetForObesityUsesIdealWeightAtTwoGramsPerKg() {
        // BMI 35 → 2.0 g/kg of ideal weight (65 kg) = 130 g, based on ideal weight.
        NutritionRegimen plan = calculator.calculate(energy(2000, 35, 65), 110, NUTRISON);
        assertThat(plan.proteinBasis()).isEqualTo("ideal weight");
        assertThat(plan.proteinTargetG()).isCloseTo(130.0, within(1e-9));
        assertThat(plan.proteinTargetPerKg()).isCloseTo(2.0, within(1e-9));
    }

    @Test
    void parenteralFibreSentinelIsNotReportedAsFibre() {
        // Parenteral formulas carry F=99 as a "not applicable" sentinel.
        NutritionProduct smof = new NutritionProduct("SK", NutritionCategory.PARENTERAL,
                "SmofKabiven®", 1.1, 5, 12.7, 3.85, 99, "1500", 92, 117, 127, 12, 10, 40.3, "");
        NutritionRegimen plan = calculator.calculate(energy(2000, 22, 65), 70, smof);
        assertThat(plan.fiberApplicable()).isFalse();
        assertThat(plan.infusionMlPerHour()).isEqualTo(76); // 2000 / (1.1 × 24)
    }
}
