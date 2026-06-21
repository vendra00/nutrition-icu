package t1tanic.nutritionicu.dto;

import t1tanic.nutritionicu.model.enums.AlertSeverity;
import t1tanic.nutritionicu.model.enums.AlertStatus;

/** Optional filters for the alerts grid; any null field means "no filter on that column". */
public record AlertFilter(AlertSeverity severity, AlertStatus status, String patientMrn, String text) {

    public static AlertFilter empty() {
        return new AlertFilter(null, null, null, null);
    }
}
