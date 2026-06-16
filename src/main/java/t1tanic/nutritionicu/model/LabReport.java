package t1tanic.nutritionicu.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One lab report = one "Petició" (order) for a patient on a given date.
 * The source PDF filename is recorded to keep ingestion idempotent.
 */
@Entity
@Table(name = "lab_report")
@Getter
@Setter
@NoArgsConstructor
public class LabReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** The lab order number (Catalan "Petició"), unique per report. */
    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    /** Internal sample/processing reference (Catalan "Referència"). */
    @Column(name = "reference")
    private String reference;

    /** "Numero Host" — instrument/host system identifier (POCT reports). */
    @Column(name = "host_number")
    private String hostNumber;

    /** "Sol·licitant" — requesting physician (may be "NO CODIFICAT"). */
    @Column(name = "requesting_physician")
    private String requestingPhysician;

    /** The centre (Catalan "Centre"), e.g. HUVH GENERAL. */
    @Column(name = "center")
    private String center;

    /** Requesting department (Catalan "Servei"), e.g. POCT-UCID1 (ICU), NEUROLOGIA. */
    @Column(name = "department")
    private String department;

    /** Patient age in years at the report: "Edat" if printed, else computed from DOB. */
    @Column(name = "age_years_at_report")
    private Integer ageYearsAtReport;

    /** Start of processing: "Recepció" (full panel) or "Data anàlisis" (POCT). */
    @Column(name = "reception_at")
    private LocalDateTime receptionAt;

    /** Completion: "Finalització" (full panel) or "Data tancament" (POCT). */
    @Column(name = "finalization_at")
    private LocalDateTime finalizationAt;

    /** The human-readable report date ("Barcelona, ... de 2024"). */
    @Column(name = "report_date")
    private LocalDate reportDate;

    /** Source PDF filename — unique guard against re-ingesting the same file. */
    @Column(name = "source_filename", nullable = false, unique = true)
    private String sourceFilename;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<ReportSection> sections = new ArrayList<>();

    public void addSection(ReportSection section) {
        section.setReport(this);
        this.sections.add(section);
    }
}
