package t1tanic.nutritionicu.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import t1tanic.nutritionicu.dto.NutritionProduct;
import t1tanic.nutritionicu.model.enums.NutritionCategory;
import tools.jackson.databind.json.JsonMapper;

/** Confirms the bundled formulary JSON loads cleanly into typed products. */
class NutritionFormularyTest {

    private final NutritionFormulary formulary = new NutritionFormulary(JsonMapper.builder().build());

    @Test
    void loadsTheWholeCatalogAcrossAllCategories() {
        assertThat(formulary.all()).hasSize(75);
        assertThat(formulary.all()).extracting(NutritionProduct::category)
                .contains(NutritionCategory.ENTERAL, NutritionCategory.PARENTERAL,
                        NutritionCategory.SUPPLEMENT);
    }

    @Test
    void productsCarryTheirComposition() {
        NutritionProduct nutrison = formulary.all().stream()
                .filter(p -> p.code().equals("NS"))
                .findFirst().orElseThrow();
        assertThat(nutrison.name()).isEqualTo("Nutrison®");
        assertThat(nutrison.densityKcalPerMl()).isEqualTo(1.0);
        assertThat(nutrison.proteinPer100ml()).isEqualTo(4.0);
        assertThat(nutrison.naMgPer100ml()).isEqualTo(100.0);
    }
}
