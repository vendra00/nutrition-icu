package t1tanic.nutritionicu.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import t1tanic.nutritionicu.model.enums.AlertSeverity;
import t1tanic.nutritionicu.model.enums.AlertStatus;
import t1tanic.nutritionicu.model.enums.Sector;

/**
 * An alert raised when a monitored patient's report contains abnormal results.
 * It targets one-to-many clinical {@link Sector}s (the doctors notified) and
 * carries the abnormal {@link LabResult}s that triggered it.
 */
@Entity
@Table(name = "alert")
@Getter
@Setter
@NoArgsConstructor
public class Alert extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /** The report whose ingestion triggered this alert. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private LabReport report;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AlertStatus status = AlertStatus.NEW;

    @Column(name = "message", length = 2000)
    private String message;

    /** Sectors whose doctors should be notified — one to many. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "alert_target_sector", joinColumns = @JoinColumn(name = "alert_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "sector", nullable = false, length = 32)
    private Set<Sector> targetSectors = new HashSet<>();

    /** The out-of-range results that triggered the alert. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "alert_abnormal_result",
            joinColumns = @JoinColumn(name = "alert_id"),
            inverseJoinColumns = @JoinColumn(name = "lab_result_id"))
    private List<LabResult> abnormalResults = new ArrayList<>();
}
