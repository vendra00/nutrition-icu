package t1tanic.nutritionicu.service.dashboard;

import java.util.List;
import t1tanic.nutritionicu.dto.DashboardStats;
import t1tanic.nutritionicu.dto.HeightWeightPoint;

/** Computes the dashboard's aggregated metrics over the monitored cohort. */
public interface DashboardService {

    DashboardStats stats();

    /** Height/weight points (with sex) of monitored patients that have both recorded — for the scatter chart. */
    List<HeightWeightPoint> heightWeightScatter();
}
