package t1tanic.nutritionicu.service.nutrition;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.model.CalorimetryMeasurement;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.repo.CalorimetryMeasurementRepository;
import t1tanic.nutritionicu.repo.PatientRepository;

@Service
public class CalorimetryServiceImpl implements CalorimetryService {

    private final CalorimetryMeasurementRepository calorimetryRepository;
    private final PatientRepository patientRepository;

    public CalorimetryServiceImpl(CalorimetryMeasurementRepository calorimetryRepository,
                                  PatientRepository patientRepository) {
        this.calorimetryRepository = calorimetryRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    @Transactional
    public CalorimetryMeasurement record(Long patientId, LocalDate date, Integer measuredKcalPerDay, Double rq) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("No patient with id " + patientId));
        CalorimetryMeasurement measurement = calorimetryRepository
                .findByPatientIdAndMeasuredOn(patientId, date)
                .orElseGet(() -> new CalorimetryMeasurement(patient, date, measuredKcalPerDay, rq));
        measurement.setMeasuredKcalPerDay(measuredKcalPerDay);
        measurement.setRq(rq);
        return calorimetryRepository.save(measurement);
    }

    @Override
    @Transactional
    public void delete(Long measurementId) {
        calorimetryRepository.findById(measurementId).ifPresent(calorimetryRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalorimetryMeasurement> history(Long patientId) {
        return calorimetryRepository.findByPatientIdOrderByMeasuredOnAsc(patientId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CalorimetryMeasurement> latest(Long patientId) {
        return calorimetryRepository.findTopByPatientIdOrderByMeasuredOnDesc(patientId);
    }
}
