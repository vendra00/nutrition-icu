package t1tanic.nutritionicu.service.nutrition;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import t1tanic.nutritionicu.model.NutritionDelivery;

/** Tracks what a patient actually received vs. what was prescribed for their feed (delivery adequacy). */
public interface NutritionDeliveryService {

    /** Records (or updates) the prescribed vs actual infusion for a date. {@code kcalPerMl} is optional. */
    NutritionDelivery record(Long patientId, LocalDate date,
                             Double prescribedMlPerHour, Double actualMlPerHour, Double kcalPerMl);

    /** Removes a delivery record. */
    void delete(Long deliveryId);

    /** A patient's delivery history, oldest first. */
    List<NutritionDelivery> history(Long patientId);

    /** The patient's most recent delivery record, if any. */
    Optional<NutritionDelivery> latest(Long patientId);
}
