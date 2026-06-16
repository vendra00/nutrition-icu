package t1tanic.nutritionicu.repo;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.Doctor;
import t1tanic.nutritionicu.model.enums.Sector;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findBySector(Sector sector);

    /** Doctors across one or more sectors — the recipients of an alert. */
    List<Doctor> findBySectorIn(Collection<Sector> sectors);
}
