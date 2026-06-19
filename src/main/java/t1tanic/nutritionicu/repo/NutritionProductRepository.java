package t1tanic.nutritionicu.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.NutritionProduct;

public interface NutritionProductRepository extends JpaRepository<NutritionProduct, Long> {

    /** All formulas in display order: category, then name. */
    List<NutritionProduct> findAllByOrderByCategoryAscNameAsc();

    boolean existsByCode(String code);
}
