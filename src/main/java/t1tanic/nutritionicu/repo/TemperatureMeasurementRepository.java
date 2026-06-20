package t1tanic.nutritionicu.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.TemperatureMeasurement;

public interface TemperatureMeasurementRepository extends JpaRepository<TemperatureMeasurement, Long> {

    /** A patient's temperatures over time, oldest first — the trend series. */
    List<TemperatureMeasurement> findByPatientIdOrderByMeasuredOnAsc(Long patientId);

    /** The entry for a specific date, if any (used to upsert on edit). */
    Optional<TemperatureMeasurement> findByPatientIdAndMeasuredOn(Long patientId, LocalDate measuredOn);

    /** The most recent dated temperature, if any — the patient's latest reading. */
    Optional<TemperatureMeasurement> findTopByPatientIdOrderByMeasuredOnDesc(Long patientId);
}
