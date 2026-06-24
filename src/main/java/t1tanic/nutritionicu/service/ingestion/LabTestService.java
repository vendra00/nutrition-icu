package t1tanic.nutritionicu.service.ingestion;

import java.util.List;
import java.util.Map;
import t1tanic.nutritionicu.dto.IngestionSummary;
import t1tanic.nutritionicu.dto.LabReportDetail;
import t1tanic.nutritionicu.dto.LabReportSummary;

/** Front-facing operations for ingesting and listing lab-report PDFs. */
public interface LabTestService {

    /** The most recently ingested reports (newest first), capped at {@code limit}. */
    List<LabReportSummary> recentReports(int limit);

    /**
     * The full contents of one report (header fields plus every section and result), for the detail view.
     *
     * @throws t1tanic.nutritionicu.exception.ResourceNotFoundException if no report has that id
     */
    LabReportDetail reportDetail(Long reportId);

    /**
     * Ingests every PDF in a subfolder of the configured ingestion root.
     *
     * @param relativePath subfolder under the root; {@code null}/blank means the root itself
     * @return a summary of what was ingested, skipped, or failed
     * @throws IllegalArgumentException if the path resolves outside the configured root
     */
    IngestionSummary ingest(String relativePath);

    /**
     * Saves browser-uploaded PDFs into the ingestion root and ingests them (so dedup, parsing and alerting
     * run exactly as for folder ingestion). Existing files are not overwritten.
     *
     * @param filesByName uploaded file name -> PDF bytes
     * @return a summary of what was ingested, skipped, or failed
     */
    IngestionSummary ingestUploaded(Map<String, byte[]> filesByName);
}
