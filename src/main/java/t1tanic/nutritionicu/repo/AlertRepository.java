package t1tanic.nutritionicu.repo;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import t1tanic.nutritionicu.model.Alert;
import t1tanic.nutritionicu.model.enums.AlertSeverity;
import t1tanic.nutritionicu.model.enums.AlertStatus;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findByPatientId(Long patientId);

    /** All alerts, newest first — most recently created leads, ties broken by id. */
    List<Alert> findAllByOrderByCreatedAtDescIdDesc();

    /** A filtered page of alerts (newest first); any null filter is ignored. */
    @Query("""
            select a from Alert a
            where (:severity is null or a.severity = :severity)
              and (:mrn is null or lower(a.patient.medicalRecordNumber) like lower(concat('%', cast(:mrn as string), '%')))
              and (:text is null or lower(a.message) like lower(concat('%', cast(:text as string), '%')))
            order by a.createdAt desc, a.id desc
            """)
    Page<Alert> search(@Param("severity") AlertSeverity severity,
                       @Param("mrn") String mrn,
                       @Param("text") String text,
                       Pageable pageable);

    @Query("""
            select count(a) from Alert a
            where (:severity is null or a.severity = :severity)
              and (:mrn is null or lower(a.patient.medicalRecordNumber) like lower(concat('%', cast(:mrn as string), '%')))
              and (:text is null or lower(a.message) like lower(concat('%', cast(:text as string), '%')))
            """)
    long countSearch(@Param("severity") AlertSeverity severity,
                     @Param("mrn") String mrn,
                     @Param("text") String text);
}
