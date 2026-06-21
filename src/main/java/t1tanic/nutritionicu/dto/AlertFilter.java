package t1tanic.nutritionicu.dto;

import t1tanic.nutritionicu.model.enums.AlertSeverity;

/** Optional filters for the alerts grid; any null field means "no filter on that column". */
public record AlertFilter(AlertSeverity severity, String patientMrn, String text) {

    public static AlertFilter empty() {
        return new AlertFilter(null, null, null);
    }
}
