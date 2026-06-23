package t1tanic.nutritionicu.service.nutrition;

import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import t1tanic.nutritionicu.exception.ConflictException;
import t1tanic.nutritionicu.exception.ValidationException;
import t1tanic.nutritionicu.model.NutritionProduct;
import t1tanic.nutritionicu.repo.NutritionProductRepository;

/**
 * Catalog of nutrition formulas, backed by the database. The built-in rccc.eu products are seeded
 * on startup (see the formulary initializer); hospital/brand products can be added, edited or removed
 * at runtime via the Nutrition formula tab.
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

    /** A single formula by id, if present. */
    public Optional<NutritionProduct> findById(Long id) {
        return repository.findById(id);
    }

    /** Persists a hospital-added formula. */
    public NutritionProduct add(NutritionProduct product) {
        product.setBuiltIn(false);
        return repository.save(product);
    }

    /**
     * Saves a new or edited formula, enforcing a unique, non-blank code.
     *
     * @throws IllegalArgumentException if the code is blank or already used by another formula
     */
    public NutritionProduct save(NutritionProduct product) {
        String code = product.getCode() == null ? null : product.getCode().strip();
        if (code == null || code.isEmpty()) {
            throw new ValidationException("Code is required");
        }
        product.setCode(code);
        repository.findByCode(code).ifPresent(existing -> {
            if (!existing.getId().equals(product.getId())) {
                throw new ConflictException("A formula with code " + code + " already exists");
            }
        });
        return repository.save(product);
    }

    /** Removes a formula by id. Built-ins re-seed on the next startup. Admin only. */
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        repository.findById(id).ifPresent(repository::delete);
    }
}
