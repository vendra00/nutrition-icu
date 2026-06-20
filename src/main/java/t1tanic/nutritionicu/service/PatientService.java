package t1tanic.nutritionicu.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import t1tanic.nutritionicu.dto.PatientDetails;
import t1tanic.nutritionicu.model.Patient;

/** Patient administrative data (stay window, etc.) and reads for the views. */
public interface PatientService {

    /** All patients, for pickers and lists. */
    List<Patient> findAll();

    /** A single patient by id, if present (e.g. to re-read after an edit). */
    Optional<Patient> findById(Long patientId);

    /** The actively monitored cohort. */
    List<Patient> findMonitored();

    /** Sets the patient's admission and discharge dates. */
    Patient updateStay(Long patientId, LocalDate admissionDate, LocalDate dischargeDate);

    /**
     * Creates a new patient from manually entered details.
     *
     * @throws IllegalArgumentException if the medical record number is blank or already in use
     */
    Patient create(PatientDetails details);

    /**
     * Updates a patient's demographic and administrative details.
     *
     * @throws IllegalArgumentException if the patient does not exist, or the medical record number
     *                                  is blank or already used by another patient
     */
    Patient updateDetails(Long patientId, PatientDetails details);
}
