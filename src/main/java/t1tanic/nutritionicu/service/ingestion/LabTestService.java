package t1tanic.nutritionicu.service.ingestion;

import t1tanic.nutritionicu.dto.IngestionSummary;

/** Front-facing operations for ingesting lab-report PDFs. */
public interface LabTestService {

    /**
     * Ingests every PDF in a subfolder of the configured ingestion root.
     *
     * @param relativePath subfolder under the root; {@code null}/blank means the root itself
     * @return a summary of what was ingested, skipped, or failed
     * @throws IllegalArgumentException if the path resolves outside the configured root
     */
    IngestionSummary ingest(String relativePath);
}
