package t1tanic.nutritionicu.service.nutrition;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.dto.EnergyExpenditureResult;
import t1tanic.nutritionicu.dto.NutritionMetrics;
import t1tanic.nutritionicu.model.EnergyAssessment;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.enums.EnergyMethod;
import t1tanic.nutritionicu.repo.EnergyAssessmentRepository;
import t1tanic.nutritionicu.repo.PatientRepository;

@Service
public class EnergyAssessmentServiceImpl implements EnergyAssessmentService {

    private final EnergyAssessmentRepository assessmentRepository;
    private final PatientRepository patientRepository;
    private final NutritionService nutritionService;

    public EnergyAssessmentServiceImpl(EnergyAssessmentRepository assessmentRepository,
                                       PatientRepository patientRepository,
                                       NutritionService nutritionService) {
        this.assessmentRepository = assessmentRepository;
        this.patientRepository = patientRepository;
        this.nutritionService = nutritionService;
    }

    @Override
    @Transactional
    public EnergyAssessment recordHarrisBenedict(Long patientId, LocalDate date,
                                                 EnergyExpenditureResult result, double actualWeightKg) {
        EnergyAssessment a = upsert(patientId, date, EnergyMethod.HARRIS_BENEDICT);
        a.setTotalKcalPerDay(result.totalKcalPerDay());
        a.setKcalPerKgPerDay(result.kcalPerKgPerDay());
        a.setWeightKg(actualWeightKg);
        a.setBmi(result.bmi());
        a.setBasalKcalPerDay(result.basalKcalPerDay());
        a.setStressFactor(result.stress());
        a.setRq(null);
        return assessmentRepository.save(a);
    }

    @Override
    @Transactional
    public EnergyAssessment recordCalorimetry(Long patientId, LocalDate date, int measuredKcalPerDay, Double rq) {
        EnergyAssessment a = upsert(patientId, date, EnergyMethod.INDIRECT_CALORIMETRY);
        NutritionMetrics metrics = nutritionService.metricsFor(a.getPatient());
        Double weight = a.getPatient().getCurrentWeightKg();
        a.setTotalKcalPerDay(measuredKcalPerDay);
        a.setKcalPerKgPerDay(weight != null && weight > 0
                ? Math.round(measuredKcalPerDay / weight * 10.0) / 10.0 : null);
        a.setWeightKg(weight);
        a.setBmi(metrics.bmi());
        a.setBasalKcalPerDay(null);
        a.setStressFactor(null);
        a.setRq(rq);
        return assessmentRepository.save(a);
    }

    @Override
    @Transactional
    public void delete(Long assessmentId) {
        assessmentRepository.findById(assessmentId).ifPresent(assessmentRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnergyAssessment> history(Long patientId) {
        return assessmentRepository.findByPatientIdOrderByAssessedOnAsc(patientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnergyAssessment> history(Long patientId, EnergyMethod method) {
        return assessmentRepository.findByPatientIdAndMethodOrderByAssessedOnAsc(patientId, method);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnergyAssessment> latest(Long patientId, EnergyMethod method) {
        return assessmentRepository.findTopByPatientIdAndMethodOrderByAssessedOnDesc(patientId, method);
    }

    /** Finds the existing (patient, date, method) row to update, or creates a new one. */
    private EnergyAssessment upsert(Long patientId, LocalDate date, EnergyMethod method) {
        return assessmentRepository.findByPatientIdAndAssessedOnAndMethod(patientId, date, method)
                .orElseGet(() -> {
                    Patient patient = patientRepository.findById(patientId)
                            .orElseThrow(() -> new IllegalArgumentException("No patient with id " + patientId));
                    return new EnergyAssessment(patient, date, method);
                });
    }
}
