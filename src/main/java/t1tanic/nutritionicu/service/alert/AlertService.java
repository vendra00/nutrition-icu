package t1tanic.nutritionicu.service.alert;

import java.util.List;
import java.util.Optional;
import t1tanic.nutritionicu.dto.AlertSummary;
import t1tanic.nutritionicu.model.Alert;
import t1tanic.nutritionicu.model.LabReport;

/** Evaluates an ingested report and raises an alert when a monitored patient has abnormal results. */
public interface AlertService {

    /**
     * Raises an alert if the report's patient is monitored and has out-of-range results.
     *
     * @return the created alert, or empty if no alert was warranted
     */
    Optional<Alert> evaluate(LabReport report);

    /**
     * Re-evaluates every report of every currently-monitored patient. Used to backfill
     * alerts after patients are flagged monitored (e.g. sandbox seeding).
     *
     * @return the number of alerts created
     */
    int evaluateForMonitoredPatients();

    /** Recent alerts as flat summaries, newest first — for the dashboard. */
    List<AlertSummary> recentAlerts();
}
