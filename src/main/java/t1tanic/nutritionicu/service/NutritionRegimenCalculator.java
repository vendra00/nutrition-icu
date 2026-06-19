package t1tanic.nutritionicu.service;

import org.springframework.stereotype.Component;
import t1tanic.nutritionicu.dto.EnergyExpenditureResult;
import t1tanic.nutritionicu.dto.NutritionProduct;
import t1tanic.nutritionicu.dto.NutritionRegimen;
import t1tanic.nutritionicu.dto.NutritionRegimen.Electrolytes;

/**
 * Turns a {@link EnergyExpenditureResult} and a chosen {@link NutritionProduct} into a 24-hour
 * administration plan, faithfully reproducing the rccc.eu calculator's {@code cX} routine.
 *
 * <p>The infusion rate delivers the day's GET over 24 hours; macros and electrolytes are scaled
 * from the formula's per-100 ml composition; the daily protein target follows the BMI band (1.5
 * g/kg of actual weight under 30, 2.0 g/kg of ideal weight at 30–40, 2.5 g/kg above 40).
 *
 * <p>Note: protein/fat/carbohydrate percentages use the source's calibrated factors (4.063 kcal/g
 * for protein and carbohydrate, 9.082 for fat) so they sum to roughly 100%.
 */
@Component
public class NutritionRegimenCalculator {

    /**
     * @param energy      a previously computed Harris-Benedict result (supplies GET, BMI, ideal weight)
     * @param actualWeightKg the patient's actual body weight, kg (&gt; 0)
     * @param product     the chosen formula
     */
    public NutritionRegimen calculate(EnergyExpenditureResult energy, double actualWeightKg,
                                      NutritionProduct product) {
        if (product == null) {
            throw new IllegalArgumentException("A nutrition product is required");
        }
        if (actualWeightKg <= 0) {
            throw new IllegalArgumentException("Actual weight must be positive");
        }

        int get = energy.totalKcalPerDay();
        double density = product.densityKcalPerMl();

        int mlPerHour = (int) Math.round(get / (density * 24.0));
        int dailyVolume = mlPerHour * 24;
        double per100 = dailyVolume / 100.0; // number of 100 ml units delivered per day

        int proteinG = (int) Math.round(per100 * product.proteinPer100ml());
        int carbG = (int) Math.round(per100 * product.carbsPer100ml());
        int fatG = (int) Math.round(per100 * product.fatPer100ml());
        double nitrogenG = round2(per100 * (product.proteinPer100ml() / 6.25));
        double fiberG = round2(per100 * product.fiberPer100ml());
        boolean fiberApplicable = fiberG <= 1000; // the source hides the absurd parenteral sentinel

        int proteinPct = (int) Math.round(product.proteinPer100ml() * 4.063 / density);
        int carbPct = (int) Math.round(product.carbsPer100ml() * 4.063 / density);
        int fatPct = (int) Math.round(product.fatPer100ml() * 9.082 / density);

        Electrolytes electrolytes = new Electrolytes(
                gramsPerDay(per100, product.naMgPer100ml()),
                gramsPerDay(per100, product.kMgPer100ml()),
                gramsPerDay(per100, product.clMgPer100ml()),
                gramsPerDay(per100, product.caMgPer100ml()),
                gramsPerDay(per100, product.mgMgPer100ml()),
                gramsPerDay(per100, product.pMgPer100ml()));

        double bmi = energy.bmi();
        double idealKg = energy.idealBodyWeightKg();
        double targetG;
        double basisWeight;
        String basis;
        if (bmi < 30) {
            targetG = actualWeightKg * 1.5;
            basisWeight = actualWeightKg;
            basis = "actual weight";
        } else if (bmi < 40) {
            targetG = idealKg * 2.0;
            basisWeight = idealKg;
            basis = "ideal weight";
        } else {
            targetG = idealKg * 2.5;
            basisWeight = idealKg;
            basis = "ideal weight";
        }
        targetG = round1(targetG);
        double targetPerKg = Math.round(targetG / basisWeight * 100.0) / 100.0;
        int deficit = targetG > proteinG ? (int) Math.round(targetG - proteinG) : 0;

        return new NutritionRegimen(product, mlPerHour, dailyVolume, get,
                proteinG, proteinPct, carbG, carbPct, fatG, fatPct,
                nitrogenG, fiberG, fiberApplicable, electrolytes,
                targetG, targetPerKg, basis, deficit);
    }

    /** mg/100 ml scaled to the day's volume and converted to grams, one decimal. */
    private static double gramsPerDay(double per100, double mgPer100ml) {
        return Math.round(per100 * mgPer100ml / 1000.0 * 10.0) / 10.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
