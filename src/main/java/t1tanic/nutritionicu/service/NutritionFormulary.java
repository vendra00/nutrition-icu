package t1tanic.nutritionicu.service;

import java.util.List;
import org.springframework.stereotype.Component;
import t1tanic.nutritionicu.model.NutritionProduct;
import t1tanic.nutritionicu.repo.NutritionProductRepository;

/**
 * Catalog of nutrition formulas, backed by the database. The built-in rccc.eu products are seeded
 * on startup (see the formulary initializer); hospital/brand products can be added at runtime.
 */
@Component
public class NutritionFormulary {

    private final NutritionProductRepository repository;

    public NutritionFormulary(NutritionProductRepository repository) {
        this.repository = repository;
    }

    /** All formulas, category then name order. */
    public List<NutritionProduct> all() {
        return repository.findAllByOrderByCategoryAscNameAsc();
    }

    /** Persists a hospital-added formula. */
    public NutritionProduct add(NutritionProduct product) {
        product.setBuiltIn(false);
        return repository.save(product);
    }
}
