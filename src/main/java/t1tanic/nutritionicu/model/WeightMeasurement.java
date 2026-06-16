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
 * A patient's weight on a given date. At most one entry per (patient, date) so that
 * recording a weight for an existing date updates it. Drives the weight trend.
 */
@Entity
@Table(name = "weight_measurement",
        uniqueConstraints = @UniqueConstraint(name = "uk_weight_patient_date",
                columnNames = {"patient_id", "measured_on"}))
@Getter
@Setter
@NoArgsConstructor
public class WeightMeasurement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "measured_on", nullable = false)
    private LocalDate measuredOn;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    public WeightMeasurement(Patient patient, LocalDate measuredOn, Double weightKg) {
        this.patient = patient;
        this.measuredOn = measuredOn;
        this.weightKg = weightKg;
    }
}
