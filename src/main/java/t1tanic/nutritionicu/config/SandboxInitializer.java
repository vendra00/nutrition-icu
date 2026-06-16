package t1tanic.nutritionicu.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import t1tanic.nutritionicu.model.Doctor;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.enums.Sector;
import t1tanic.nutritionicu.repo.DoctorRepository;
import t1tanic.nutritionicu.repo.PatientRepository;
import t1tanic.nutritionicu.service.AlertService;

/**
 * Seeds sandbox/demo data on startup: a handful of doctors, and (for now) marks
 * every ingested patient as monitored so alert/insight features have a live cohort.
 * Runs after ingestion. Disable with {@code app.sandbox.seed=false}.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.sandbox.seed", havingValue = "true", matchIfMissing = true)
public class SandboxInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SandboxInitializer.class);

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AlertService alertService;

    public SandboxInitializer(PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              AlertService alertService) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.alertService = alertService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedDoctors();
        markAllPatientsMonitored();
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
}
