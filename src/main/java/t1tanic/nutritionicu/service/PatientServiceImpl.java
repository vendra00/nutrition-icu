package t1tanic.nutritionicu.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.repo.PatientRepository;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    public PatientServiceImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    @Transactional
    public Patient updateStay(Long patientId, LocalDate admissionDate, LocalDate dischargeDate) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("No patient with id " + patientId));
        patient.setAdmissionDate(admissionDate);
        patient.setDischargeDate(dischargeDate);
        return patientRepository.save(patient);
    }
}
