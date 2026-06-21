package t1tanic.nutritionicu.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.NutritionDelivery;

public interface NutritionDeliveryRepository extends JpaRepository<NutritionDelivery, Long> {

    /** A patient's delivery records over time, oldest first — the trend series. */
    List<NutritionDelivery> findByPatientIdOrderByMeasuredOnAsc(Long patientId);

    /** The record for a specific date, if any (used to upsert on edit). */
    Optional<NutritionDelivery> findByPatientIdAndMeasuredOn(Long patientId, LocalDate measuredOn);

    /** The most recent dated record, if any — the patient's latest delivery. */
    Optional<NutritionDelivery> findTopByPatientIdOrderByMeasuredOnDesc(Long patientId);
}
