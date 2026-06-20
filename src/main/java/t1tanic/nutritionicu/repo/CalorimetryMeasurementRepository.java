package t1tanic.nutritionicu.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.CalorimetryMeasurement;

public interface CalorimetryMeasurementRepository extends JpaRepository<CalorimetryMeasurement, Long> {

    /** A patient's calorimetry studies over time, oldest first — the trend series. */
    List<CalorimetryMeasurement> findByPatientIdOrderByMeasuredOnAsc(Long patientId);

    /** The study for a specific date, if any (used to upsert on edit). */
    Optional<CalorimetryMeasurement> findByPatientIdAndMeasuredOn(Long patientId, LocalDate measuredOn);

    /** The most recent dated study, if any — the patient's latest measured energy expenditure. */
    Optional<CalorimetryMeasurement> findTopByPatientIdOrderByMeasuredOnDesc(Long patientId);
}
