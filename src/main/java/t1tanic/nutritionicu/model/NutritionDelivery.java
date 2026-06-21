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
 * What a patient actually received vs. what was prescribed for a day's enteral/parenteral feed.
 * Patients frequently get less than the prescribed infusion (interruptions, intolerance, procedures),
 * so tracking the real delivery lets clinicians see feeding adequacy (% delivered) over time. At most
 * one entry per (patient, date). {@code kcalPerMl} (the formula's caloric density) is optional and only
 * used to express delivery in kcal/day.
 */
@Entity
@Table(name = "nutrition_delivery",
        uniqueConstraints = @UniqueConstraint(name = "uk_delivery_patient_date",
                columnNames = {"patient_id", "measured_on"}))
@Getter
@Setter
@NoArgsConstructor
public class NutritionDelivery extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "measured_on", nullable = false)
    private LocalDate measuredOn;

    /** Prescribed infusion rate, ml/h. */
    @Column(name = "prescribed_ml_per_hour", nullable = false)
    private Double prescribedMlPerHour;

    /** Actually delivered infusion rate, ml/h. */
    @Column(name = "actual_ml_per_hour", nullable = false)
    private Double actualMlPerHour;

    /** Formula caloric density, kcal/ml — optional, for expressing delivery in kcal/day. */
    @Column(name = "kcal_per_ml")
    private Double kcalPerMl;

    public NutritionDelivery(Patient patient, LocalDate measuredOn,
                             Double prescribedMlPerHour, Double actualMlPerHour, Double kcalPerMl) {
        this.patient = patient;
        this.measuredOn = measuredOn;
        this.prescribedMlPerHour = prescribedMlPerHour;
        this.actualMlPerHour = actualMlPerHour;
        this.kcalPerMl = kcalPerMl;
    }

    /** Fraction of the prescribed volume actually delivered, as a percentage, or null if not computable. */
    public Double percentDelivered() {
        if (prescribedMlPerHour == null || prescribedMlPerHour <= 0 || actualMlPerHour == null) {
            return null;
        }
        return actualMlPerHour / prescribedMlPerHour * 100.0;
    }
}
