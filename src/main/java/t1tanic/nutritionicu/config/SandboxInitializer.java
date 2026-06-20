package t1tanic.nutritionicu.config;

import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import t1tanic.nutritionicu.model.Doctor;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.enums.AdmissionDelayBand;
import t1tanic.nutritionicu.model.enums.ApacheBand;
import t1tanic.nutritionicu.model.enums.ComorbidityBand;
import t1tanic.nutritionicu.model.enums.Il6Band;
import t1tanic.nutritionicu.model.enums.Sector;
import t1tanic.nutritionicu.model.enums.Sex;
import t1tanic.nutritionicu.model.enums.SofaBand;
import t1tanic.nutritionicu.repo.DoctorRepository;
import t1tanic.nutritionicu.repo.PatientRepository;
import t1tanic.nutritionicu.service.AlertService;
import t1tanic.nutritionicu.service.NutritionService;

/**
 * Seeds sandbox/demo data on startup: a handful of doctors, and (for now) marks
 * every ingested patient as monitored so alert/insight features have a live cohort.
 * Runs after ingestion. Disable with {@code app.sandbox.seed=false}.
 */
@Slf4j
@Component
@Order(2)
@ConditionalOnProperty(name = "app.sandbox.seed", havingValue = "true", matchIfMissing = true)
public class SandboxInitializer implements ApplicationRunner {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AlertService alertService;
    private final NutritionService nutritionService;

    public SandboxInitializer(PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              AlertService alertService,
                              NutritionService nutritionService) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.alertService = alertService;
        this.nutritionService = nutritionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedDoctors();
        markAllPatientsMonitored();
        seedAnthropometryAndWeights();
        seedTemperatures();
        seedRiskAssessments();
        int alerts = alertService.evaluateForMonitoredPatients();
        log.info("Sandbox: raised {} alert(s) from monitored patients' reports", alerts);
    }

    private void seedDoctors() {
        if (doctorRepository.count() > 0) {
            return;
        }
        List<Doctor> doctors = List.of(
                new Doctor("Dr. Anna Vidal", Sector.ICU),
                new Doctor("Dr. Carlos Lázaro", Sector.NEUROLOGY),
                new Doctor("Dr. Marta Soler", Sector.CARDIOLOGY),
                new Doctor("Dr. Jordi Puig", Sector.NEPHROLOGY),
                new Doctor("Dr. Elena Marín", Sector.INTERNAL_MEDICINE));
        doctorRepository.saveAll(doctors);
        log.info("Sandbox: seeded {} doctors", doctors.size());
    }

    private void markAllPatientsMonitored() {
        List<Patient> patients = patientRepository.findAll();
        patients.forEach(patient -> patient.setMonitored(true));
        patientRepository.saveAll(patients);
        log.info("Sandbox: marked {} patient(s) as monitored", patients.size());
    }

    /**
     * Gives each patient a plausible height/usual weight (by sex, varied per id) and a
     * 5-week weight series trending down slightly — so BMI and the weight trend chart
     * have demo data. Skips patients that already have a height recorded.
     */
    private void seedAnthropometryAndWeights() {
        List<Patient> patients = patientRepository.findByMonitoredTrue();
        int seeded = 0;
        for (Patient patient : patients) {
            if (patient.getHeightCm() != null || patient.getId() == null) {
                continue;
            }
            boolean male = patient.getSex() == Sex.MALE;
            long n = patient.getId();
            double height = (male ? 175 : 162) + (n % 11) - 5;   // ±5 cm
            double usual = (male ? 82 : 66) + (n % 9) - 4;       // ±4 kg
            nutritionService.updateAnthropometry(patient.getId(), height, usual);

            LocalDate start = LocalDate.of(2024, 7, 1).plusDays(n % 5);
            double weight = usual - 1;
            for (int week = 0; week < 5; week++) {
                double w = Math.round((weight - week * 0.8) * 10.0) / 10.0; // gentle decline
                nutritionService.recordWeight(patient.getId(), start.plusWeeks(week), w);
            }
            seeded++;
        }
        log.info("Sandbox: seeded anthropometry + weight series for {} patient(s)", seeded);
    }

    /**
     * Gives each patient a 10-day daily body-temperature series with a plausible mid-window febrile
     * episode (peak ≈ 39 °C) settling back to normal — so the temperature trend has demo data.
     * Tracking only; not used in any calculation. Idempotent: skips patients that already have any.
     */
    private void seedTemperatures() {
        // Days from a normal baseline: rises into a fever spike, then settles.
        double[] deltas = {0.1, 0.4, 0.9, 1.7, 2.2, 1.5, 0.8, 0.4, 0.2, 0.0};
        List<Patient> patients = patientRepository.findByMonitoredTrue();
        int seeded = 0;
        for (Patient patient : patients) {
            if (patient.getId() == null || !nutritionService.temperatureHistory(patient.getId()).isEmpty()) {
                continue;
            }
            long n = patient.getId();
            double base = 36.7 + (n % 5) * 0.1;                       // 36.7–37.1 baseline
            LocalDate start = LocalDate.now().minusDays(deltas.length - 1 + (int) (n % 3));
            for (int day = 0; day < deltas.length; day++) {
                double t = Math.round((base + deltas[day]) * 10.0) / 10.0;
                nutritionService.recordTemperature(patient.getId(), start.plusDays(day), t);
            }
            seeded++;
        }
        log.info("Sandbox: seeded temperature series for {} patient(s)", seeded);
    }

    /**
     * Records one NUTRIC assessment per patient, cycling four severity profiles (low → very high) by id
     * so the demo shows a realistic spread of scores. Age band is derived from the patient. Idempotent:
     * skips patients that already have an assessment.
     */
    private void seedRiskAssessments() {
        List<Patient> patients = patientRepository.findByMonitoredTrue();
        int seeded = 0;
        for (Patient patient : patients) {
            if (patient.getId() == null || nutritionService.latestRiskAssessment(patient.getId()).isPresent()) {
                continue;
            }
            long n = patient.getId();
            LocalDate assessedOn = LocalDate.now().minusDays(n % 4);
            switch ((int) (n % 4)) {
                case 0 -> nutritionService.recordRiskAssessment(patient.getId(), assessedOn,
                        ApacheBand.LT_15, SofaBand.LT_6, ComorbidityBand.LE_1, AdmissionDelayBand.LT_1, null);
                case 1 -> nutritionService.recordRiskAssessment(patient.getId(), assessedOn,
                        ApacheBand.B15_19, SofaBand.B6_9, ComorbidityBand.LE_1, AdmissionDelayBand.LT_1, null);
                case 2 -> nutritionService.recordRiskAssessment(patient.getId(), assessedOn,
                        ApacheBand.B20_27, SofaBand.GE_10, ComorbidityBand.GE_2, AdmissionDelayBand.GE_1, null);
                default -> nutritionService.recordRiskAssessment(patient.getId(), assessedOn,
                        ApacheBand.GE_28, SofaBand.GE_10, ComorbidityBand.GE_2, AdmissionDelayBand.GE_1, Il6Band.GE_400);
            }
            seeded++;
        }
        log.info("Sandbox: seeded NUTRIC risk assessments for {} patient(s)", seeded);
    }
}
