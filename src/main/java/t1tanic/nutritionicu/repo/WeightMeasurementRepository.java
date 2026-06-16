package t1tanic.nutritionicu.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.WeightMeasurement;

public interface WeightMeasurementRepository extends JpaRepository<WeightMeasurement, Long> {

    /** A patient's weights over time, oldest first — the trend series. */
    List<WeightMeasurement> findByPatientIdOrderByMeasuredOnAsc(Long patientId);

    /** The entry for a specific date, if any (used to upsert on edit). */
    Optional<WeightMeasurement> findByPatientIdAndMeasuredOn(Long patientId, LocalDate measuredOn);
}
