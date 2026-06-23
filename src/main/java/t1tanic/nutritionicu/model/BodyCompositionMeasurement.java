package t1tanic.nutritionicu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A patient's body-composition reading on a given date (typically from bioimpedance/DXA): body fat %,
 * skeletal muscle mass %, bone density and phase angle. Any field may be null (the doctor records what they
 * have). At most one entry per (patient, date) so re-recording a date updates it. Recorded because BMI alone
 * can mislead in muscular patients; this is the objective body-composition context.
 */
@Entity
@Table(name = "body_composition_measurement",
        uniqueConstraints = @UniqueConstraint(name = "uk_bodycomp_patient_date",
                columnNames = {"patient_id", "measured_on"}))
@Getter
@Setter
@NoArgsConstructor
public class BodyCompositionMeasurement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "measured_on", nullable = false)
    private LocalDate measuredOn;

    /** Body fat as a percentage of total body mass. */
    @Column(name = "body_fat_percent")
    private Double bodyFatPercent;

    /** Skeletal muscle mass as a percentage of total body mass. */
    @Column(name = "skeletal_muscle_percent")
    private Double skeletalMusclePercent;

    /** Bone mineral density (g/cm²). */
    @Column(name = "bone_density")
    private Double boneDensity;

    /** Bioimpedance phase angle (degrees). */
    @Column(name = "phase_angle")
    private Double phaseAngle;

    public BodyCompositionMeasurement(Patient patient, LocalDate measuredOn) {
        this.patient = patient;
        this.measuredOn = measuredOn;
    }
}
