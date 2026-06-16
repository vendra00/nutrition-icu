package t1tanic.nutritionicu.model.enums;

/**
 * How urgent an alert is, derived from the worst flag among its abnormal results:
 * a doubled arrow (VERY_HIGH/VERY_LOW) is CRITICAL, a single arrow is WARNING.
 */
public enum AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}
