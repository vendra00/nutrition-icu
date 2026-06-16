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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A block of results within a report, mirroring how the PDF is laid out.
 * Reports are assembled dynamically from whatever the doctor ordered, so a
 * report holds a varying set of sections, each with its own list of analytes.
 *
 * <p>{@code category} is the top-level heading (HEMATOLOGIA, BIOQUIMICA,
 * GASOS EN SANG); {@code name} is the sub-heading (HEMOGRAMA, IONS, SUBSTRATS,
 * ENZIMS…). The "Resultats revisats i validats per:" line is printed once per
 * section, so the validator lives here rather than on each result.
 */
@Entity
@Table(name = "report_section")
@Getter
@Setter
@NoArgsConstructor
public class ReportSection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private LabReport report;

    /** Top-level heading, e.g. HEMATOLOGIA, BIOQUIMICA, GASOS EN SANG. */
    @Column(name = "category", nullable = false)
    private String category;

    /** Sub-heading within the category, e.g. HEMOGRAMA, IONS, SUBSTRATS. May be null. */
    @Column(name = "name")
    private String name;

    /** "Resultats revisats i validats per:" — the validating professional. */
    @Column(name = "validated_by")
    private String validatedBy;

    /** Position of this section within the report, preserving printed order. */
    @Column(name = "sequence")
    private Integer sequence;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<LabResult> results = new ArrayList<>();

    public ReportSection(String category, String name) {
        this.category = category;
        this.name = name;
    }

    public void addResult(LabResult result) {
        result.setSection(this);
        this.results.add(result);
    }
}
