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
 * An indirect-calorimetry study for a patient on a given date: the measured (resting) energy
 * expenditure in kcal/day and the respiratory quotient (RQ = VCO2/VO2). At most one entry per
 * (patient, date) so re-recording a date updates it. IC is repeated every few days through a stay,
 * so these accumulate as a series.
 */
@Entity
@Table(name = "calorimetry_measurement",
        uniqueConstraints = @UniqueConstraint(name = "uk_calorimetry_patient_date",
                columnNames = {"patient_id", "measured_on"}))
@Getter
@Setter
@NoArgsConstructor
public class CalorimetryMeasurement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "measured_on", nullable = false)
    private LocalDate measuredOn;

    /** Measured energy expenditure (mEE / REE), kcal/day. */
    @Column(name = "measured_kcal_per_day", nullable = false)
    private Integer measuredKcalPerDay;

    /** Respiratory quotient (VCO2/VO2); physiologic range ~0.66–1.2. Optional. */
    @Column(name = "rq")
    private Double rq;

    public CalorimetryMeasurement(Patient patient, LocalDate measuredOn, Integer measuredKcalPerDay, Double rq) {
        this.patient = patient;
        this.measuredOn = measuredOn;
        this.measuredKcalPerDay = measuredKcalPerDay;
        this.rq = rq;
    }
}
