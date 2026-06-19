package t1tanic.nutritionicu.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import t1tanic.nutritionicu.model.NutritionProduct;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Loads the bundled rccc.eu formulary from {@code nutrition-formulas.json}. These are the built-in
 * products used to seed the catalog on startup; hospital-added products live only in the database.
 */
public final class NutritionFormularyData {

    private NutritionFormularyData() {
    }

    /** Parses the bundled formulas into (transient) entities; no database involved. */
    public static List<NutritionProduct> bundled(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource("nutrition-formulas.json").getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<NutritionProduct>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Could not load nutrition-formulas.json", e);
        }
    }
}
