package t1tanic.nutritionicu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import t1tanic.nutritionicu.model.enums.AdmissionDelayBand;
import t1tanic.nutritionicu.model.enums.AgeBand;
import t1tanic.nutritionicu.model.enums.ApacheBand;
import t1tanic.nutritionicu.model.enums.ComorbidityBand;
import t1tanic.nutritionicu.model.enums.Il6Band;
import t1tanic.nutritionicu.model.enums.SofaBand;

/**
 * A dated nutritional-risk assessment. The severity components are recorded as
 * banded selections; the resulting NUTRIC score/classification is stored alongside.
 */
@Entity
@Table(name = "nutrition_risk_assessment")
@Getter
@Setter
@NoArgsConstructor
public class NutritionRiskAssessment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "assessed_on", nullable = false)
    private LocalDate assessedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_band", length = 16)
    private AgeBand ageBand;

    @Enumerated(EnumType.STRING)
    @Column(name = "apache_band", length = 16)
    private ApacheBand apacheBand;

    @Enumerated(EnumType.STRING)
    @Column(name = "sofa_band", length = 16)
    private SofaBand sofaBand;

    @Enumerated(EnumType.STRING)
    @Column(name = "comorbidity_band", length = 16)
    private ComorbidityBand comorbidityBand;

    @Enumerated(EnumType.STRING)
    @Column(name = "admission_delay_band", length = 16)
    private AdmissionDelayBand admissionDelayBand;

    /** Optional — when present the 10-point NUTRIC variant is used. */
    @Enumerated(EnumType.STRING)
    @Column(name = "il6_band", length = 16)
    private Il6Band il6Band;

    @Column(name = "nutric_score")
    private Integer nutricScore;

    @Column(name = "nutric_max")
    private Integer nutricMax;

    @Column(name = "high_risk", nullable = false)
    private boolean highRisk;

    public NutritionRiskAssessment(Patient patient, LocalDate assessedOn) {
        this.patient = patient;
        this.assessedOn = assessedOn;
    }
}
