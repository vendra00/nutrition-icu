package t1tanic.nutritionicu.dto;

/**
 * Anthropometry-derived values for the nutrition protocol. Any field may be null
 * when its inputs (height/weights/sex) are missing.
 *
 * @param bmi                   body mass index (kg/m²)
 * @param idealBodyWeightKg     ideal body weight (Devine), used in metabolic equations
 * @param adjustedBodyWeightKg  adjusted body weight (for obesity); equals current weight when not overweight
 * @param weightLossPercent     recent weight loss vs usual weight, % (positive = loss)
 */
public record NutritionMetrics(
        Double bmi,
        Double idealBodyWeightKg,
        Double adjustedBodyWeightKg,
        Double weightLossPercent) {
}
