package t1tanic.nutritionicu.service;

import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import t1tanic.nutritionicu.dto.IngestionSummary;

/**
 * Resolves the front-end's requested folder against a configured root, guards
 * against escaping that root, and delegates the actual work to the ingestion engine.
 */
@Slf4j
@Service
public class LabTestServiceImpl implements LabTestService {

    private final LabReportIngestionService ingestionService;
    private final Path root;

    public LabTestServiceImpl(LabReportIngestionService ingestionService,
                              @Value("${app.ingestion.root:src/main/resources/data}") String root) {
        this.ingestionService = ingestionService;
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public IngestionSummary ingest(String relativePath) {
        Path target = resolveWithinRoot(relativePath);
        log.info("Ingesting from {}", target);
        return ingestionService.ingestDirectory(target);
    }

    /** Resolves a relative subfolder under the root, rejecting anything that escapes it. */
    private Path resolveWithinRoot(String relativePath) {
        String relative = relativePath == null ? "" : relativePath.strip();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException(
                    "Path '" + relativePath + "' resolves outside the ingestion root");
        }
        return target;
    }
}
