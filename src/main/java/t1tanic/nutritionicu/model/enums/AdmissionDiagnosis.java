package t1tanic.nutritionicu.model.enums;

/**
 * ICU admission diagnosis category. Drives the kind of nutrition course expected and is used to prefer
 * same-condition cases when comparing a patient against the archive. Labels are English; once app-wide
 * i18n lands these can resolve per locale.
 */
public enum AdmissionDiagnosis {

    TBI("Traumatic brain injury (TBI)"),
    TBI_POLYTRAUMA("Severe TBI + polytrauma"),
    ACUTE_SPINAL_CORD_INJURY("Acute traumatic spinal cord injury"),
    POLYTRAUMA_NO_SEVERE_TBI("Polytrauma without severe TBI"),
    MAJOR_BURNS("Major burns"),
    SEPTIC_SHOCK("Septic shock"),
    ACUTE_RESPIRATORY_FAILURE("Acute respiratory failure"),
    COMPLEX_POSTOP("Complex postoperative"),
    OTHER_NEUROLOGICAL("Other neurological"),
    OTHER_MEDICAL("Other medical");

    private final String label;

    AdmissionDiagnosis(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
