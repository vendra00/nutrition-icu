package t1tanic.nutritionicu.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.Alert;
import t1tanic.nutritionicu.model.enums.AlertStatus;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findByPatientId(Long patientId);

    /** All alerts, newest first — most recently created leads, ties broken by id. */
    List<Alert> findAllByOrderByCreatedAtDescIdDesc();
}
