package t1tanic.nutritionicu.dto;

/**
 * Aggregated metrics for the dashboard, computed over the monitored cohort. Counts are small (one ICU
 * unit), so this is recomputed on each dashboard load rather than cached.
 */
public record DashboardStats(
        int monitoredPatients,
        int activeAlerts,
        int criticalAlerts,
        int warningAlerts,
        int highRisk,
        int lowRisk,
        int notAssessed,
        int underweight,
        int normalWeight,
        int overweight,
        int obese,
        Integer avgPercentDelivered) {
}
