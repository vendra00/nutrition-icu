package t1tanic.nutritionicu.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.model.Doctor;
import t1tanic.nutritionicu.repo.DoctorRepository;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return doctorRepository.count();
    }
}
