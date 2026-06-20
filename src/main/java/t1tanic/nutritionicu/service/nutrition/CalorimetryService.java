package t1tanic.nutritionicu.service.nutrition;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import t1tanic.nutritionicu.model.CalorimetryMeasurement;

/** Indirect-calorimetry studies: the measured energy expenditure (and RQ) recorded over a patient's stay. */
public interface CalorimetryService {

    /** Records (or updates) a study for a date. {@code rq} is optional. */
    CalorimetryMeasurement record(Long patientId, LocalDate date, Integer measuredKcalPerDay, Double rq);

    /** Removes a study. */
    void delete(Long measurementId);

    /** A patient's calorimetry history, oldest first. */
    List<CalorimetryMeasurement> history(Long patientId);

    /** The patient's most recent calorimetry study, if any. */
    Optional<CalorimetryMeasurement> latest(Long patientId);
}
