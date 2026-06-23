package t1tanic.nutritionicu.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.BodyCompositionMeasurement;

public interface BodyCompositionMeasurementRepository
        extends JpaRepository<BodyCompositionMeasurement, Long> {

    /** A patient's body-composition readings over time, oldest first — the trend series. */
    List<BodyCompositionMeasurement> findByPatientIdOrderByMeasuredOnAsc(Long patientId);

    /** The entry for a specific date, if any (used to upsert on edit). */
    Optional<BodyCompositionMeasurement> findByPatientIdAndMeasuredOn(Long patientId, LocalDate measuredOn);

    /** The most recent dated reading, if any — the patient's latest body composition. */
    Optional<BodyCompositionMeasurement> findTopByPatientIdOrderByMeasuredOnDesc(Long patientId);
}
