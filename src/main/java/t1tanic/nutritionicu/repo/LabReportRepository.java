package t1tanic.nutritionicu.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.LabReport;

public interface LabReportRepository extends JpaRepository<LabReport, Long> {

    /** Idempotency guard: a file is ingested at most once. */
    boolean existsBySourceFilename(String sourceFilename);

    /** A report (order number) may also arrive under a differently named duplicate file. */
    boolean existsByOrderNumber(String orderNumber);

    List<LabReport> findByPatientId(Long patientId);
}
