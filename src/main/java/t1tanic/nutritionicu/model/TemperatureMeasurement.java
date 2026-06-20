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
 * A patient's body temperature (°C) on a given date. At most one entry per (patient, date) so that
 * recording a temperature for an existing date updates it. Tracked for context/trend only — it does
 * not feed any energy or nutrition calculation.
 */
@Entity
@Table(name = "temperature_measurement",
        uniqueConstraints = @UniqueConstraint(name = "uk_temperature_patient_date",
                columnNames = {"patient_id", "measured_on"}))
@Getter
@Setter
@NoArgsConstructor
public class TemperatureMeasurement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "measured_on", nullable = false)
    private LocalDate measuredOn;

    @Column(name = "temperature_c", nullable = false)
    private Double temperatureCelsius;

    public TemperatureMeasurement(Patient patient, LocalDate measuredOn, Double temperatureCelsius) {
        this.patient = patient;
        this.measuredOn = measuredOn;
        this.temperatureCelsius = temperatureCelsius;
    }
}
