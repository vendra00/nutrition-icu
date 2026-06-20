package t1tanic.nutritionicu.service.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import t1tanic.nutritionicu.model.NutritionProduct;
import t1tanic.nutritionicu.model.enums.NutritionCategory;
import tools.jackson.databind.json.JsonMapper;

/** Confirms the bundled formulary JSON parses cleanly into products (no database involved). */
class NutritionFormularyDataTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void loadsTheWholeCatalogAcrossAllCategories() {
        assertThat(NutritionFormularyData.bundled(mapper)).hasSize(75);
        assertThat(NutritionFormularyData.bundled(mapper)).extracting(NutritionProduct::getCategory)
                .contains(NutritionCategory.ENTERAL, NutritionCategory.PARENTERAL,
                        NutritionCategory.SUPPLEMENT);
    }

    @Test
    void productsCarryTheirComposition() {
        NutritionProduct nutrison = NutritionFormularyData.bundled(mapper).stream()
                .filter(p -> p.getCode().equals("NS"))
                .findFirst().orElseThrow();
        assertThat(nutrison.getName()).isEqualTo("Nutrison®");
        assertThat(nutrison.getDensityKcalPerMl()).isEqualTo(1.0);
        assertThat(nutrison.getProteinPer100ml()).isEqualTo(4.0);
        assertThat(nutrison.getSodiumMgPer100ml()).isEqualTo(100.0);
        assertThat(nutrison.getPotassiumMgPer100ml()).isEqualTo(150.0);
        assertThat(nutrison.getPhosphorusMgPer100ml()).isEqualTo(72.0);
    }
}
