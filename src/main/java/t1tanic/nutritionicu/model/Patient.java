package t1tanic.nutritionicu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Patient(String medicalRecordNumber) {
        this.medicalRecordNumber = medicalRecordNumber;
    }
}
