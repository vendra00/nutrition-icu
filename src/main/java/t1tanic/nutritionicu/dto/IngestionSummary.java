package t1tanic.nutritionicu.dto;

import java.util.List;

/**
 * Outcome of an ingestion run over a folder.
 *
 * @param ingested files newly parsed and stored
 * @param skipped  files already present (by filename or Petició)
 * @param failed   files that errored
 * @param errors   one "filename: message" per failed file
 */
public record IngestionSummary(int ingested, int skipped, int failed, List<String> errors) {
}
