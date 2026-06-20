package t1tanic.nutritionicu.service;

import java.util.List;
import java.util.Map;
import t1tanic.nutritionicu.model.LabResult;

/** Read access to a patient's lab results for the analytics and metabolic-monitoring views. */
public interface LabResultService {

    /** A patient's readings for one canonical analyte code, oldest first. */
    List<LabResult> seriesByCode(Long patientId, String analyteCode);

    /** A patient's readings for one raw analyte label, oldest first. */
    List<LabResult> seriesByName(Long patientId, String analyteName);

    /** Each analyte label this patient has results for, mapped to its (first non-null) raw unit. */
    Map<String, String> analyteUnits(Long patientId);
}
