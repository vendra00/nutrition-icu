package t1tanic.nutritionicu.dto;

import t1tanic.nutritionicu.model.LabReport;
import t1tanic.nutritionicu.model.Patient;

/**
 * Result of parsing one PDF: a detached {@link Patient} (identified by NHC) and a
 * detached {@link LabReport} with its sections/results attached. The ingestion
 * service reconciles the patient against the database before persisting.
 */
public record ParsedReport(Patient patient, LabReport report) {
}
