package t1tanic.nutritionicu.dto;

import java.util.List;

/**
 * Outcome of an ingestion run over a set of PDFs.
 *
 * @param ingested files newly parsed and stored
 * @param skipped  files already present (by filename or Petició)
 * @param failed   files that errored
 * @param errors   one "filename: message" per failed file
 * @param results  the per-file outcome, in the order processed
 */
public record IngestionSummary(int ingested, int skipped, int failed, List<String> errors,
                               List<FileResult> results) {

    /** What happened to a single file. */
    public enum Outcome {
        INGESTED,
        SKIPPED,
        FAILED
    }

    /** A single file's outcome; {@code message} carries the failure reason when {@code outcome == FAILED}. */
    public record FileResult(String filename, Outcome outcome, String message) {
    }
}
