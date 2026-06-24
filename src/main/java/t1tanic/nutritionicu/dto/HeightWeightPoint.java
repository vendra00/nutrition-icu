package t1tanic.nutritionicu.dto;

import t1tanic.nutritionicu.model.enums.Sex;

/** One monitored patient's height/weight point for the dashboard scatter chart, tagged by sex. */
public record HeightWeightPoint(Sex sex, double heightCm, double weightKg) {
}
