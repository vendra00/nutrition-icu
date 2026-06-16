package t1tanic.nutritionicu.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import t1tanic.nutritionicu.service.LabTestService;

/**
 * Ingests the ingestion root once at startup. Enabled by default; disable with
 * {@code app.ingestion.on-startup=false} to ingest only via the REST endpoint.
 * Runs before the sandbox seeder so patients exist before they're flagged.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "app.ingestion.on-startup", havingValue = "true", matchIfMissing = true)
public class IngestionStartupRunner implements ApplicationRunner {

    private final LabTestService labTestService;

    public IngestionStartupRunner(LabTestService labTestService) {
        this.labTestService = labTestService;
    }

    @Override
    public void run(ApplicationArguments args) {
        labTestService.ingest(null); // null => the configured root
    }
}
