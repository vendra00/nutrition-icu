package t1tanic.nutritionicu.dto;

import java.time.LocalDate;
import t1tanic.nutritionicu.model.enums.AdmissionDiagnosis;
import t1tanic.nutritionicu.model.enums.Sex;

/**
 * Editable demographic and administrative fields of a patient, as entered manually by a
 * clinician in the Patients tab. Anthropometry (height/weight) and the stay window are
 * edited separately and are intentionally not part of this form.
 */
public record PatientDetails(
        String medicalRecordNumber,
        String fullName,
        LocalDate birthDate,
        Sex sex,
        String healthCardId,
        String socialSecurityNumber,
        AdmissionDiagnosis admissionDiagnosis,
        boolean monitored) {
}
