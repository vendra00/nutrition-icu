package t1tanic.nutritionicu.service.dashboard;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.dto.AlertSummary;
import t1tanic.nutritionicu.dto.DashboardStats;
import t1tanic.nutritionicu.dto.HeightWeightPoint;
import t1tanic.nutritionicu.dto.NutritionMetrics;
import t1tanic.nutritionicu.dto.PatientRef;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.service.alert.AlertService;
import t1tanic.nutritionicu.service.nutrition.NutritionDeliveryService;
import t1tanic.nutritionicu.service.nutrition.NutritionService;
import t1tanic.nutritionicu.service.patient.PatientService;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final PatientService patientService;
    private final NutritionService nutritionService;
    private final NutritionDeliveryService deliveryService;
    private final AlertService alertService;

    public DashboardServiceImpl(PatientService patientService,
                                NutritionService nutritionService,
                                NutritionDeliveryService deliveryService,
                                AlertService alertService) {
        this.patientService = patientService;
        this.nutritionService = nutritionService;
        this.deliveryService = deliveryService;
        this.alertService = alertService;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStats stats() {
        List<Patient> patients = patientService.findMonitored();

        int highRisk = 0;
        int lowRisk = 0;
        int notAssessed = 0;
        int underweight = 0;
        int normalWeight = 0;
        int overweight = 0;
        int obese = 0;
        double deliverySum = 0;
        int deliveryCount = 0;

        for (Patient patient : patients) {
            var risk = nutritionService.latestRiskAssessment(patient.getId());
            if (risk.isEmpty()) {
                notAssessed++;
            } else if (risk.get().isHighRisk()) {
                highRisk++;
            } else {
                lowRisk++;
            }

            NutritionMetrics metrics = nutritionService.metricsFor(patient);
            Double bmi = metrics.bmi();
            if (bmi != null) {
                if (bmi < 18.5) {
                    underweight++;
                } else if (bmi < 25) {
                    normalWeight++;
                } else if (bmi < 30) {
                    overweight++;
                } else {
                    obese++;
                }
            }

            var delivery = deliveryService.latest(patient.getId());
            if (delivery.isPresent() && delivery.get().percentDelivered() != null) {
                deliverySum += delivery.get().percentDelivered();
                deliveryCount++;
            }
        }

        List<AlertSummary> alerts = alertService.recentAlerts();
        int critical = (int) alerts.stream().filter(a -> "CRITICAL".equals(a.severity())).count();
        int warning = (int) alerts.stream().filter(a -> "WARNING".equals(a.severity())).count();
        Integer avgDelivered = deliveryCount == 0 ? null : (int) Math.round(deliverySum / deliveryCount);

        return new DashboardStats(patients.size(), alerts.size(), critical, warning,
                highRisk, lowRisk, notAssessed,
                underweight, normalWeight, overweight, obese, avgDelivered);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HeightWeightPoint> heightWeightScatter() {
        return patientService.findMonitored().stream()
                .filter(p -> p.getHeightCm() != null && p.getCurrentWeightKg() != null
                        && p.getHeightCm() > 0 && p.getCurrentWeightKg() > 0)
                .map(p -> new HeightWeightPoint(p.getSex(), p.getHeightCm(), p.getCurrentWeightKg()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NutricBuckets nutricBuckets() {
        List<PatientRef> high = new ArrayList<>();
        List<PatientRef> low = new ArrayList<>();
        List<PatientRef> notAssessed = new ArrayList<>();
        for (Patient p : patientService.findMonitored()) {
            var risk = nutritionService.latestRiskAssessment(p.getId());
            if (risk.isEmpty()) {
                notAssessed.add(ref(p));
            } else if (risk.get().isHighRisk()) {
                high.add(ref(p));
            } else {
                low.add(ref(p));
            }
        }
        return new NutricBuckets(high, low, notAssessed);
    }

    @Override
    @Transactional(readOnly = true)
    public BmiBuckets bmiBuckets() {
        List<PatientRef> underweight = new ArrayList<>();
        List<PatientRef> normal = new ArrayList<>();
        List<PatientRef> overweight = new ArrayList<>();
        List<PatientRef> obese = new ArrayList<>();
        for (Patient p : patientService.findMonitored()) {
            Double bmi = nutritionService.metricsFor(p).bmi();
            if (bmi == null) {
                continue;
            }
            if (bmi < 18.5) {
                underweight.add(ref(p));
            } else if (bmi < 25) {
                normal.add(ref(p));
            } else if (bmi < 30) {
                overweight.add(ref(p));
            } else {
                obese.add(ref(p));
            }
        }
        return new BmiBuckets(underweight, normal, overweight, obese);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryTrend> deliveryTrends() {
        List<DeliveryTrend> trends = new ArrayList<>();
        for (Patient p : patientService.findMonitored()) {
            List<DeliveryPoint> points = deliveryService.history(p.getId()).stream()
                    .filter(d -> d.percentDelivered() != null)
                    .map(d -> new DeliveryPoint(d.getMeasuredOn(), d.percentDelivered()))
                    .toList();
            if (!points.isEmpty()) {
                trends.add(new DeliveryTrend(p.getMedicalRecordNumber(), points));
            }
        }
        return trends;
    }

    private static PatientRef ref(Patient p) {
        return new PatientRef(p.getId(), p.getMedicalRecordNumber(), p.getFullName());
    }
}
