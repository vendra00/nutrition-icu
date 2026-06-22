package t1tanic.nutritionicu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import t1tanic.nutritionicu.model.enums.Sex;

/**
 * An anonymized, completed "case" snapshot used as the comparison cohort for AI insights. It holds the
 * de-identified comparison features (age, BMI, NUTRIC, SOFA) as indexed columns for fast similarity
 * search, plus a precomputed de-identified course narrative so comparison needs no per-peer recompute.
 *
 * <p>No name or NHC is stored. {@code sourcePatientId} is kept only so re-archiving updates the same
 * case (and to avoid comparing a patient to its own case); it should be dropped/hashed for a truly
 * severed anonymized export.
 */
@Entity
@Table(name = "patient_case", indexes = {
        @Index(name = "idx_patient_case_age", columnList = "age_years"),
        @Index(name = "idx_patient_case_bmi", columnList = "bmi"),
        @Index(name = "idx_patient_case_nutric", columnList = "nutric_score"),
        @Index(name = "idx_patient_case_source", columnList = "source_patient_id")
})
@Getter
@Setter
@NoArgsConstructor
public class PatientCase extends BaseEntity {

    /** Anonymized human-readable label, e.g. "CASE-0007". */
    @Column(name = "case_code", length = 32)
    private String caseCode;

    /** Internal back-reference for idempotent re-archiving; not part of the de-identified payload. */
    @Column(name = "source_patient_id")
    private Long sourcePatientId;

    @Column(name = "age_years")
    private Integer ageYears;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", length = 16)
    private Sex sex;

    @Column(name = "bmi")
    private Double bmi;

    @Column(name = "nutric_score")
    private Integer nutricScore;

    @Column(name = "nutric_max")
    private Integer nutricMax;

    @Column(name = "high_risk")
    private Boolean highRisk;

    /** Ordinal of the SOFA band (for distance) and its label (for display). */
    @Column(name = "sofa_ordinal")
    private Integer sofaOrdinal;

    @Column(name = "sofa_band", length = 16)
    private String sofaBand;

    @Column(name = "length_of_stay_days")
    private Integer lengthOfStayDays;

    @Column(name = "discharged")
    private Boolean discharged;

    /** Precomputed de-identified course narrative (features + stay/outcome + lab/weight trajectories). */
    @Column(name = "course_text", columnDefinition = "text")
    private String courseText;
}
