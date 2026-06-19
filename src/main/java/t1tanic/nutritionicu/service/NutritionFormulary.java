package t1tanic.nutritionicu.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import t1tanic.nutritionicu.dto.NutritionProduct;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * In-memory catalog of nutrition formulas, loaded once from {@code nutrition-formulas.json}
 * (transcribed from the rccc.eu formulary). Ordered by category then name for display.
 */
@Component
public class NutritionFormulary {

    private final List<NutritionProduct> products;

    public NutritionFormulary(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource("nutrition-formulas.json").getInputStream()) {
            this.products = objectMapper
                    .readValue(in, new TypeReference<List<NutritionProduct>>() {})
                    .stream()
                    .sorted(Comparator.comparing(NutritionProduct::category)
                            .thenComparing(NutritionProduct::name))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Could not load nutrition-formulas.json", e);
        }
    }

    /** All formulas, category then name order. */
    public List<NutritionProduct> all() {
        return products;
    }
}
