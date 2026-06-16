package t1tanic.nutritionicu.service;

import java.time.LocalDate;
import t1tanic.nutritionicu.model.Patient;

/** Patient administrative data (stay window, etc.). */
public interface PatientService {

    /** Sets the patient's admission and discharge dates. */
    Patient updateStay(Long patientId, LocalDate admissionDate, LocalDate dischargeDate);
}
