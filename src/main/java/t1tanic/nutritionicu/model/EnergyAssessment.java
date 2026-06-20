package t1tanic.nutritionicu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import t1tanic.nutritionicu.model.enums.EnergyMethod;
import t1tanic.nutritionicu.model.enums.StressFactor;

/**
 * A dated energy-expenditure assessment for a patient, from either the Harris-Benedict equation or an
 * indirect-calorimetry study. Storing both methods in one comparable structure lets us track a patient's
 * energy targets over time and compare measured-vs-predicted for research. At most one entry per
 * (patient, date, method). Method-specific fields are null for the other method.
 */
@Entity
@Table(name = "energy_assessment",
        uniqueConstraints = @UniqueConstraint(name = "uk_energy_patient_date_method",
                columnNames = {"patient_id", "assessed_on", "method"}))
@Getter
@Setter
@NoArgsConstructor
public class EnergyAssessment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "assessed_on", nullable = false)
    private LocalDate assessedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 32)
    private EnergyMethod method;

    /** Total energy expenditure used as the target: GET (Harris-Benedict) or mEE (calorimetry), kcal/day. */
    @Column(name = "total_kcal_per_day", nullable = false)
    private Integer totalKcalPerDay;

    /** Total divided by actual body weight, kcal/kg/day. */
    @Column(name = "kcal_per_kg_per_day")
    private Double kcalPerKgPerDay;

    /** Actual body weight at the time of assessment, kg. */
    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "bmi")
    private Double bmi;

    // --- Harris-Benedict only ---

    /** Basal energy expenditure (GEB), kcal/day. */
    @Column(name = "basal_kcal_per_day")
    private Integer basalKcalPerDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "stress_factor", length = 32)
    private StressFactor stressFactor;

    // --- Indirect calorimetry only ---

    /** Respiratory quotient (VCO2/VO2). */
    @Column(name = "rq")
    private Double rq;

    public EnergyAssessment(Patient patient, LocalDate assessedOn, EnergyMethod method) {
        this.patient = patient;
        this.assessedOn = assessedOn;
        this.method = method;
    }
}
