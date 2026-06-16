package t1tanic.nutritionicu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import t1tanic.nutritionicu.model.enums.ResultFlag;

/**
 * A single measured analyte within a report (analyte-per-row model).
 * Keeps both the raw printed value and a parsed numeric form so non-numeric
 * results ("&lt;3", "&gt;90") survive while numeric ones stay queryable for trends.
 */
@Entity
@Table(name = "lab_result", indexes = {
        @Index(name = "idx_lab_result_analyte_observed", columnList = "analyte_name, observed_at"),
        // Core trend query: one canonical analyte for one patient over time.
        @Index(name = "idx_lab_result_patient_analyte", columnList = "patient_id, analyte_code, observed_at")
})
@Getter
@Setter
@NoArgsConstructor
public class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private ReportSection section;

    /**
     * Owning patient, denormalized from the report chain so per-patient trend
     * queries don't need to join through report_section/lab_report. Just the FK,
     * not patient fields — set during ingestion.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** Raw analyte label as printed, e.g. "Pla-Glucosa", "vSan-Plasma; ió Sodi". */
    @Column(name = "analyte_name", nullable = false)
    private String analyteName;

    /** Optional canonical code assigned during normalization (for insights). */
    @Column(name = "analyte_code")
    private String analyteCode;

    /** Value exactly as printed, e.g. "130", "&lt;3", "&gt;90". */
    @Column(name = "value_raw")
    private String valueRaw;

    /** Parsed numeric value, null when non-numeric. */
    @Column(name = "value_numeric", precision = 18, scale = 4)
    private BigDecimal valueNumeric;

    /** Comparison operator when the value is bounded, e.g. "&lt;" or "&gt;". */
    @Column(name = "value_operator", length = 2)
    private String valueOperator;

    @Column(name = "unit")
    private String unit;

    /** Lower bound of the reference range, when given. */
    @Column(name = "ref_low", precision = 18, scale = 4)
    private BigDecimal refLow;

    /** Upper bound of the reference range, when given. */
    @Column(name = "ref_high", precision = 18, scale = 4)
    private BigDecimal refHigh;

    /** Reference range exactly as printed (covers non-numeric ranges too). */
    @Column(name = "ref_raw")
    private String refRaw;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag", length = 16)
    private ResultFlag flag = ResultFlag.NORMAL;

    /**
     * When this value was measured — denormalized from the parent report's
     * effective timestamp so per-analyte trends are queryable on this row alone.
     * Indexed together with analyte_name for time-series lookups.
     */
    @Column(name = "observed_at")
    private LocalDateTime observedAt;

    /** Position within the section, preserving the printed order. */
    @Column(name = "sequence")
    private Integer sequence;
}
