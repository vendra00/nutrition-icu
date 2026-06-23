package t1tanic.nutritionicu.service.patient;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.dto.PatientDetails;
import t1tanic.nutritionicu.exception.ConflictException;
import t1tanic.nutritionicu.exception.ResourceNotFoundException;
import t1tanic.nutritionicu.exception.ValidationException;
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
    @Transactional(readOnly = true)
    public List<Patient> findAll() {
        return patientRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Patient> findById(Long patientId) {
        return patientRepository.findById(patientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findMonitored() {
        return patientRepository.findByMonitoredTrue();
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
                .orElseThrow(() -> new ResourceNotFoundException("No patient with id " + patientId));
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
        patient.setAdmissionDiagnosis(details.admissionDiagnosis());
        patient.setMonitored(details.monitored());
        patient.setAdmissionDate(details.admissionDate());
        patient.setDischargeDate(details.dischargeDate());
        patient.setMisleadingBmi(details.misleadingBmi());
    }

    private static String requireNhc(PatientDetails details) {
        String nhc = details.medicalRecordNumber() == null ? null : details.medicalRecordNumber().strip();
        if (nhc == null || nhc.isEmpty()) {
            throw new ValidationException("Medical record number (NHC) is required");
        }
        return nhc;
    }

    /** Rejects an NHC already used by a different patient ({@code selfId} may be null on create). */
    private void requireNhcAvailable(String nhc, Long selfId) {
        patientRepository.findByMedicalRecordNumber(nhc)
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new ConflictException("A patient with NHC " + nhc + " already exists");
                });
    }
}
