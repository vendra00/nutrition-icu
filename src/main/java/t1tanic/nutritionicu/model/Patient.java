package t1tanic.nutritionicu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.Period;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import t1tanic.nutritionicu.model.enums.AdmissionDiagnosis;
import t1tanic.nutritionicu.model.enums.Sex;

/**
 * A patient, deduplicated across reports by their clinical history number (NHC).
 * Holds only the stable demographic fields printed on the report header.
 */
@Entity
@Table(name = "patient")
@Getter
@Setter
@NoArgsConstructor
public class Patient extends BaseEntity {

    /** Medical record number (Catalan "NHC") — stable hospital-wide patient identifier. */
    @Column(name = "medical_record_number", nullable = false, unique = true)
    private String medicalRecordNumber;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false, length = 16)
    private Sex sex = Sex.UNKNOWN;

    /** Health-card id (Catalan "CIP" / Targeta Sanitària) — CatSalut personal code. */
    @Column(name = "health_card_id")
    private String healthCardId;

    @Column(name = "social_security_number")
    private String socialSecurityNumber;

    /** ICU admission diagnosis category ("motivo de ingreso") — relevant to the expected nutrition course. */
    @Enumerated(EnumType.STRING)
    @Column(name = "admission_diagnosis", length = 40)
    private AdmissionDiagnosis admissionDiagnosis;

    /**
     * Whether the patient is currently admitted and under active monitoring (e.g. in the ICU).
     * When true, incoming results are evaluated for alerts and feed progression insights.
     * Managed by clinicians via the system, not inferred from historical reports.
     */
    @Column(name = "monitored", nullable = false)
    private boolean monitored = false;

    // --- Anthropometry (nutrition screening; entered by the doctor) ---

    /** Height in centimetres. */
    @Column(name = "height_cm")
    private Double heightCm;

    /** Current (admission) weight in kilograms. */
    @Column(name = "current_weight_kg")
    private Double currentWeightKg;

    /** Usual/habitual weight in kilograms — to assess recent weight loss. */
    @Column(name = "usual_weight_kg")
    private Double usualWeightKg;

    // --- Stay window ---

    /** Date the patient was admitted. */
    @Column(name = "admission_date")
    private LocalDate admissionDate;

    /** Date the patient was discharged/released (null while still admitted). */
    @Column(name = "discharge_date")
    private LocalDate dischargeDate;

    public Patient(String medicalRecordNumber) {
        this.medicalRecordNumber = medicalRecordNumber;
    }

    /** Age in whole years as of {@code date}, or {@code null} if the birth date is unknown. */
    public Integer ageOn(LocalDate date) {
        return birthDate == null ? null : Period.between(birthDate, date).getYears();
    }
}
