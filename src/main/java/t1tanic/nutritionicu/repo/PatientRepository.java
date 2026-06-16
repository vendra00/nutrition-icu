package t1tanic.nutritionicu.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber);
}
