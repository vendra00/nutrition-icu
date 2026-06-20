package t1tanic.nutritionicu.service.patient;

import t1tanic.nutritionicu.dto.PatientOverview;

/** Gathers a patient's key nutrition values + basic stats and renders them as a downloadable PDF. */
public interface PatientOverviewService {

    /**
     * Builds the overview snapshot for a patient.
     *
     * @throws IllegalArgumentException if the patient does not exist
     */
    PatientOverview build(Long patientId);

    /** Renders an overview as a one-page PDF document. */
    byte[] toPdf(PatientOverview overview);
}
