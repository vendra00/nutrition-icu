package t1tanic.nutritionicu.dto;

import java.time.LocalDate;
import t1tanic.nutritionicu.model.enums.AdmissionDiagnosis;
import t1tanic.nutritionicu.model.enums.Sex;

/**
 * A gathered snapshot of a patient's most important nutrition values and basic stats, for the
 * Patients-tab overview dialog and its downloadable PDF. Any field may be null when unrecorded.
 */
public record PatientOverview(Identity identity, Anthropometry anthropometry, Risk risk) {

    public record Identity(String medicalRecordNumber, String fullName, Sex sex, Integer ageYears,
                           LocalDate birthDate, AdmissionDiagnosis admissionDiagnosis, boolean monitored,
                           LocalDate admissionDate, LocalDate dischargeDate) {
    }

    public record Anthropometry(Double heightCm, Double currentWeightKg, Double usualWeightKg,
                                Double bmi, boolean misleadingBmi,
                                Double idealBodyWeightKg, Double adjustedBodyWeightKg,
                                Double weightLossPercent,
                                Double latestTemperatureC, LocalDate latestTemperatureDate) {
    }

    public record Risk(Integer nutricScore, Integer nutricMax, Boolean highRisk, LocalDate assessedOn) {

        /** Whether a NUTRIC assessment exists at all. */
        public boolean present() {
            return nutricScore != null;
        }
    }
}
