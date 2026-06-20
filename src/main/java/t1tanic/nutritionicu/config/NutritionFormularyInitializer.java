package t1tanic.nutritionicu.config;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import t1tanic.nutritionicu.model.NutritionProduct;
import t1tanic.nutritionicu.repo.NutritionProductRepository;
import t1tanic.nutritionicu.service.nutrition.NutritionFormularyData;
import tools.jackson.databind.ObjectMapper;

/**
 * Seeds the built-in rccc.eu formulary into the database on startup, idempotently: a product is
 * inserted only if its code isn't already present, so hospital-added products are left untouched
 * (and, once the schema is no longer recreated each run, persist across restarts).
 */
@Slf4j
@Component
@Order(0)
public class NutritionFormularyInitializer implements ApplicationRunner {

    private final NutritionProductRepository repository;
    private final ObjectMapper objectMapper;

    public NutritionFormularyInitializer(NutritionProductRepository repository,
                                         ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<NutritionProduct> toSeed = NutritionFormularyData.bundled(objectMapper).stream()
                .filter(p -> !repository.existsByCode(p.getCode()))
                .peek(p -> p.setBuiltIn(true))
                .toList();
        if (toSeed.isEmpty()) {
            return;
        }
        repository.saveAll(toSeed);
        log.info("Seeded {} built-in nutrition formula(s)", toSeed.size());
    }
}
