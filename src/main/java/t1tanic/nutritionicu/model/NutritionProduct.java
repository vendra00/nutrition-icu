package t1tanic.nutritionicu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import t1tanic.nutritionicu.model.enums.NutritionCategory;

/**
 * A nutrition formula in the catalog. Macronutrients are grams per 100 ml and electrolytes are
 * milligrams per 100 ml, matching the rccc.eu formulary the built-ins are seeded from.
 *
 * <p>Persisted so hospital/brand products can be added at runtime alongside the seeded built-ins
 * ({@link #builtIn}). The energy/regimen calculations read these values directly.
 */
@Entity
@Table(name = "nutrition_product")
@Getter
@Setter
@NoArgsConstructor
public class NutritionProduct extends BaseEntity {

    /** Short selector code; unique across the catalog. */
    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NutritionCategory category;

    @Column(nullable = false)
    private String name;

    /** Caloric density, kcal/ml. */
    @Column(nullable = false)
    private double densityKcalPerMl;

    /** Protein, g/100 ml. */
    private double proteinPer100ml;

    /** Carbohydrate, g/100 ml. */
    private double carbsPer100ml;

    /** Fat, g/100 ml. */
    private double fatPer100ml;

    /** Fibre, g/100 ml (99 is the rccc.eu "not applicable" sentinel, e.g. for parenteral). */
    private double fiberPer100ml;

    /** Osmolarity as printed (mOsm/l); may be a range or null. */
    private String osmolarity;

    private double sodiumMgPer100ml;
    private double potassiumMgPer100ml;
    private double chlorideMgPer100ml;
    private double magnesiumMgPer100ml;
    private double calciumMgPer100ml;
    private double phosphorusMgPer100ml;

    /** Free-text clinical indications. */
    @Column(length = 2048)
    private String indications;

    /** True for the seeded rccc.eu formulary; false for products added at the hospital. */
    @Column(nullable = false)
    private boolean builtIn;

    /** Mirrors the original rccc.eu field order; {@link #builtIn} defaults to false (hospital-added). */
    public NutritionProduct(String code, NutritionCategory category, String name,
                            double densityKcalPerMl, double proteinPer100ml, double carbsPer100ml,
                            double fatPer100ml, double fiberPer100ml, String osmolarity,
                            double sodiumMgPer100ml, double potassiumMgPer100ml, double chlorideMgPer100ml,
                            double magnesiumMgPer100ml, double calciumMgPer100ml, double phosphorusMgPer100ml,
                            String indications) {
        this.code = code;
        this.category = category;
        this.name = name;
        this.densityKcalPerMl = densityKcalPerMl;
        this.proteinPer100ml = proteinPer100ml;
        this.carbsPer100ml = carbsPer100ml;
        this.fatPer100ml = fatPer100ml;
        this.fiberPer100ml = fiberPer100ml;
        this.osmolarity = osmolarity;
        this.sodiumMgPer100ml = sodiumMgPer100ml;
        this.potassiumMgPer100ml = potassiumMgPer100ml;
        this.chlorideMgPer100ml = chlorideMgPer100ml;
        this.magnesiumMgPer100ml = magnesiumMgPer100ml;
        this.calciumMgPer100ml = calciumMgPer100ml;
        this.phosphorusMgPer100ml = phosphorusMgPer100ml;
        this.indications = indications;
    }
}
