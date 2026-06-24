package t1tanic.nutritionicu.service.dashboard;

import java.time.LocalDate;
import java.util.List;
import t1tanic.nutritionicu.dto.DashboardStats;
import t1tanic.nutritionicu.dto.HeightWeightPoint;
import t1tanic.nutritionicu.dto.PatientRef;

/** Computes the dashboard's aggregated metrics over the monitored cohort. */
public interface DashboardService {

    DashboardStats stats();

    /** Height/weight points (with sex) of monitored patients that have both recorded — for the scatter chart. */
    List<HeightWeightPoint> heightWeightScatter();

    /** Monitored patients grouped by NUTRIC risk bucket — for the pie's drill-down. */
    NutricBuckets nutricBuckets();

    /** Monitored patients grouped by WHO BMI band — for the BMI chart's drill-down. */
    BmiBuckets bmiBuckets();

    /** Per-patient nutrition-delivery (% of prescribed) trajectories — for the area-spline trend. */
    List<DeliveryTrend> deliveryTrends();

    record NutricBuckets(List<PatientRef> highRisk, List<PatientRef> lowRisk, List<PatientRef> notAssessed) {
    }

    record BmiBuckets(List<PatientRef> underweight, List<PatientRef> normalWeight,
                      List<PatientRef> overweight, List<PatientRef> obese) {
    }

    record DeliveryTrend(String label, List<DeliveryPoint> points) {
    }

    record DeliveryPoint(LocalDate date, double percent) {
    }
}
