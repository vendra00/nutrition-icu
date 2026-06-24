package t1tanic.nutritionicu.repo;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import t1tanic.nutritionicu.dto.LabReportSummary;
import t1tanic.nutritionicu.model.LabReport;

public interface LabReportRepository extends JpaRepository<LabReport, Long> {

    /** Idempotency guard: a file is ingested at most once. */
    boolean existsBySourceFilename(String sourceFilename);

    /** A report (order number) may also arrive under a differently named duplicate file. */
    boolean existsByOrderNumber(String orderNumber);

    List<LabReport> findByPatientId(Long patientId);

    /** Ingested reports as flat summary rows, newest-ingested first — for the Lab reports table. */
    @Query("""
            select new t1tanic.nutritionicu.dto.LabReportSummary(
                r.id, p.id, r.orderNumber, p.medicalRecordNumber, p.fullName, r.department,
                r.reportDate, r.receptionAt, size(r.sections), r.sourceFilename, r.createdAt)
            from LabReport r join r.patient p
            order by r.createdAt desc, r.id desc
            """)
    List<LabReportSummary> findRecentSummaries(Pageable pageable);
}
