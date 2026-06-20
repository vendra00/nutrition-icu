package t1tanic.nutritionicu.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import t1tanic.nutritionicu.model.LabResult;

public interface LabResultRepository extends JpaRepository<LabResult, Long> {

    /** A patient's readings for one analyte, oldest first — the trend series. */
    List<LabResult> findByPatientIdAndAnalyteNameOrderByObservedAtAsc(Long patientId, String analyteName);

    /**
     * A patient's readings for one canonical analyte code, oldest first. Use this when a
     * marker is printed under several raw labels (e.g. {@code Pla-} vs {@code Srm-} variants)
     * that all map to one code — the metabolic-monitoring panel relies on this.
     */
    List<LabResult> findByPatientIdAndAnalyteCodeOrderByObservedAtAsc(Long patientId, String analyteCode);

    /**
     * Distinct (analyte label, raw unit) pairs for a patient, so the picker can group analytes by
     * unit. A label may appear with a null unit and a real one across reports; the caller keeps the
     * first non-null.
     */
    @Query("select distinct r.analyteName, r.unitRaw from LabResult r where r.patient.id = :patientId")
    List<Object[]> findAnalyteNameUnits(@Param("patientId") Long patientId);
}
