package t1tanic.nutritionicu.service.lab;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.model.LabResult;
import t1tanic.nutritionicu.repo.LabResultRepository;

@Service
public class LabResultServiceImpl implements LabResultService {

    private final LabResultRepository resultRepository;

    public LabResultServiceImpl(LabResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabResult> seriesByCode(Long patientId, String analyteCode) {
        return resultRepository.findByPatientIdAndAnalyteCodeOrderByObservedAtAsc(patientId, analyteCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabResult> seriesByName(Long patientId, String analyteName) {
        return resultRepository.findByPatientIdAndAnalyteNameOrderByObservedAtAsc(patientId, analyteName);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> analyteUnits(Long patientId) {
        Map<String, String> unitByName = new HashMap<>();
        for (Object[] row : resultRepository.findAnalyteNameUnits(patientId)) {
            unitByName.compute((String) row[0], (name, current) -> current != null ? current : (String) row[1]);
        }
        return unitByName;
    }
}
