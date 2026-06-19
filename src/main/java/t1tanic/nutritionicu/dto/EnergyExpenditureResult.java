package t1tanic.nutritionicu.dto;

import t1tanic.nutritionicu.model.enums.StressFactor;
import t1tanic.nutritionicu.model.enums.WeightClass;

/**
 * Result of a Harris-Benedict energy-expenditure calculation (rccc.eu method).
 *
 * @param bmi                body mass index (kg/m²), rounded to one decimal
 * @param weightClass        BMI band that decided which weight fed the equation
 * @param idealBodyWeightKg  averaged ideal body weight (PI), rounded to one decimal
 * @param weightUsedKg       weight actually fed to the equation (actual or obesity-adjusted)
 * @param basalKcalPerDay    GEB — basal energy expenditure, kcal/day
 * @param totalKcalPerDay    GET — total energy expenditure = GEB × (0.1 + stress factor), kcal/day
 * @param kcalPerKgPerDay    GET per kg of actual body weight, rounded to one decimal
 * @param stress             stress degree applied
 */
public record EnergyExpenditureResult(
        double bmi,
        WeightClass weightClass,
        double idealBodyWeightKg,
        double weightUsedKg,
        int basalKcalPerDay,
        int totalKcalPerDay,
        double kcalPerKgPerDay,
        StressFactor stress) {
}
