package t1tanic.nutritionicu.service.nutrition;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.model.NutritionDelivery;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.repo.NutritionDeliveryRepository;
import t1tanic.nutritionicu.repo.PatientRepository;

@Service
public class NutritionDeliveryServiceImpl implements NutritionDeliveryService {

    private final NutritionDeliveryRepository deliveryRepository;
    private final PatientRepository patientRepository;

    public NutritionDeliveryServiceImpl(NutritionDeliveryRepository deliveryRepository,
                                        PatientRepository patientRepository) {
        this.deliveryRepository = deliveryRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    @Transactional
    public NutritionDelivery record(Long patientId, LocalDate date,
                                    Double prescribedMlPerHour, Double actualMlPerHour, Double kcalPerMl) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("No patient with id " + patientId));
        NutritionDelivery delivery = deliveryRepository.findByPatientIdAndMeasuredOn(patientId, date)
                .orElseGet(() -> new NutritionDelivery(patient, date,
                        prescribedMlPerHour, actualMlPerHour, kcalPerMl));
        delivery.setPrescribedMlPerHour(prescribedMlPerHour);
        delivery.setActualMlPerHour(actualMlPerHour);
        delivery.setKcalPerMl(kcalPerMl);
        return deliveryRepository.save(delivery);
    }

    @Override
    @Transactional
    public void delete(Long deliveryId) {
        deliveryRepository.findById(deliveryId).ifPresent(deliveryRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NutritionDelivery> history(Long patientId) {
        return deliveryRepository.findByPatientIdOrderByMeasuredOnAsc(patientId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NutritionDelivery> latest(Long patientId) {
        return deliveryRepository.findTopByPatientIdOrderByMeasuredOnDesc(patientId);
    }
}
