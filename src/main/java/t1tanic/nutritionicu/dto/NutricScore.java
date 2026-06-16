package t1tanic.nutritionicu.dto;

/**
 * NUTRIC (Nutrition Risk in the Critically ill) result.
 *
 * @param score        total points
 * @param maxScore     9 without IL-6, 10 with IL-6
 * @param highRisk     true when the patient is high nutritional risk (≥5 without IL-6, ≥6 with)
 * @param includesIl6  whether IL-6 was part of the calculation
 */
public record NutricScore(int score, int maxScore, boolean highRisk, boolean includesIl6) {
}
