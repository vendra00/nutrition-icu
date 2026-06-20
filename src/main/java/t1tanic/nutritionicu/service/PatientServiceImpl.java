package t1tanic.nutritionicu.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.dto.PatientDetails;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.enums.Sex;
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

    @Override
    @Transactional
    public Patient create(PatientDetails details) {
        String nhc = requireNhc(details);
        requireNhcAvailable(nhc, null);
        Patient patient = new Patient(nhc);
        apply(patient, details);
        return patientRepository.save(patient);
    }

    @Override
    @Transactional
    public Patient updateDetails(Long patientId, PatientDetails details) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("No patient with id " + patientId));
        String nhc = requireNhc(details);
        requireNhcAvailable(nhc, patientId);
        patient.setMedicalRecordNumber(nhc);
        apply(patient, details);
        return patientRepository.save(patient);
    }

    /** Copies the editable fields (everything except the NHC) onto the entity. */
    private static void apply(Patient patient, PatientDetails details) {
        patient.setFullName(details.fullName());
        patient.setBirthDate(details.birthDate());
        patient.setSex(details.sex() == null ? Sex.UNKNOWN : details.sex());
        patient.setHealthCardId(details.healthCardId());
        patient.setSocialSecurityNumber(details.socialSecurityNumber());
        patient.setMonitored(details.monitored());
    }

    private static String requireNhc(PatientDetails details) {
        String nhc = details.medicalRecordNumber() == null ? null : details.medicalRecordNumber().strip();
        if (nhc == null || nhc.isEmpty()) {
            throw new IllegalArgumentException("Medical record number (NHC) is required");
        }
        return nhc;
    }

    /** Rejects an NHC already used by a different patient ({@code selfId} may be null on create). */
    private void requireNhcAvailable(String nhc, Long selfId) {
        patientRepository.findByMedicalRecordNumber(nhc)
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A patient with NHC " + nhc + " already exists");
                });
    }
}
