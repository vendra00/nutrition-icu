package t1tanic.nutritionicu.dto;

import t1tanic.nutritionicu.model.NutritionProduct;

/**
 * A 24-hour administration plan for a chosen {@link NutritionProduct} that delivers a target
 * energy expenditure (GET), reproducing the rccc.eu "Pauta de administración" step.
 *
 * @param product             the selected formula
 * @param infusionMlPerHour   continuous infusion rate to deliver GET over 24 h
 * @param dailyVolumeMl       resulting 24-hour volume
 * @param totalKcal           energy delivered (= GET), kcal/day
 * @param proteinG            protein delivered, g/day
 * @param proteinPercent      protein as % of the formula's calories
 * @param carbG               carbohydrate delivered, g/day
 * @param carbPercent         carbohydrate as % of calories
 * @param fatG                fat delivered, g/day
 * @param fatPercent          fat as % of calories
 * @param nitrogenG           nitrogen delivered (protein / 6.25), g/day
 * @param fiberG              fibre delivered, g/day (only meaningful when {@code fiberApplicable})
 * @param fiberApplicable     false for formulas with no fibre figure (e.g. parenteral)
 * @param electrolytes        electrolytes delivered, g/day
 * @param proteinTargetG      recommended daily protein for this BMI band, g/day
 * @param proteinTargetPerKg  that target expressed per kg of the basis weight
 * @param proteinBasis        which weight the target is based on ("actual" or "ideal")
 * @param proteinDeficitG     target minus delivered protein when positive, else 0
 */
public record NutritionRegimen(
        NutritionProduct product,
        int infusionMlPerHour,
        int dailyVolumeMl,
        int totalKcal,
        int proteinG,
        int proteinPercent,
        int carbG,
        int carbPercent,
        int fatG,
        int fatPercent,
        double nitrogenG,
        double fiberG,
        boolean fiberApplicable,
        Electrolytes electrolytes,
        double proteinTargetG,
        double proteinTargetPerKg,
        String proteinBasis,
        int proteinDeficitG) {

    /** Electrolytes delivered over 24 hours, grams. */
    public record Electrolytes(double sodiumG, double potassiumG, double chlorideG,
                               double calciumG, double magnesiumG, double phosphorusG) {
    }
}
