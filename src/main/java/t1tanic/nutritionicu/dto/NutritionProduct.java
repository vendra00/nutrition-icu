package t1tanic.nutritionicu.dto;

import t1tanic.nutritionicu.model.enums.NutritionCategory;

/**
 * A nutrition formula from the rccc.eu formulary. Macronutrients are grams per 100 ml and
 * electrolytes are milligrams per 100 ml, as published by the source calculator.
 *
 * @param code               short selector code from the source page
 * @param category           enteral / parenteral / supplement
 * @param name               product (brand) name
 * @param densityKcalPerMl   caloric density, kcal/ml
 * @param proteinPer100ml    protein, g/100 ml
 * @param carbsPer100ml      carbohydrate, g/100 ml
 * @param fatPer100ml        fat, g/100 ml
 * @param fiberPer100ml      fibre, g/100 ml (99 is the source's "not applicable" sentinel)
 * @param osmolarity         osmolarity as printed (mOsm/l), may be a range or null
 * @param naMgPer100ml       sodium, mg/100 ml
 * @param kMgPer100ml        potassium, mg/100 ml
 * @param clMgPer100ml       chloride, mg/100 ml
 * @param mgMgPer100ml       magnesium, mg/100 ml
 * @param caMgPer100ml       calcium, mg/100 ml
 * @param pMgPer100ml        phosphorus, mg/100 ml
 * @param indications        free-text clinical indications (Spanish, as published)
 */
public record NutritionProduct(
        String code,
        NutritionCategory category,
        String name,
        double densityKcalPerMl,
        double proteinPer100ml,
        double carbsPer100ml,
        double fatPer100ml,
        double fiberPer100ml,
        String osmolarity,
        double naMgPer100ml,
        double kMgPer100ml,
        double clMgPer100ml,
        double mgMgPer100ml,
        double caMgPer100ml,
        double pMgPer100ml,
        String indications) {
}
