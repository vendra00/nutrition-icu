package t1tanic.nutritionicu.service.dashboard;

import t1tanic.nutritionicu.dto.DashboardStats;

/** Computes the dashboard's aggregated metrics over the monitored cohort. */
public interface DashboardService {

    DashboardStats stats();
}
