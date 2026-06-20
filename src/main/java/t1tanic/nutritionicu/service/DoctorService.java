package t1tanic.nutritionicu.service;

import java.util.List;
import t1tanic.nutritionicu.model.Doctor;

/** Read access to the doctor directory. */
public interface DoctorService {

    /** All doctors. */
    List<Doctor> findAll();

    /** How many doctors are registered. */
    long count();
}
