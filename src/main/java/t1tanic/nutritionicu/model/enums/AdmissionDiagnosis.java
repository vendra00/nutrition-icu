package t1tanic.nutritionicu.model.enums;

/**
 * ICU admission diagnosis category ("motivo de ingreso"). Drives the kind of nutrition course expected
 * and is used to prefer same-condition cases when comparing a patient against the archive. Labels are
 * the Spanish terms used by the unit.
 */
public enum AdmissionDiagnosis {

    TBI("TCE"),
    TBI_POLYTRAUMA("TCE + Politrauma grave"),
    ACUTE_SPINAL_CORD_INJURY("Lesión medular aguda traumática"),
    POLYTRAUMA_NO_SEVERE_TBI("Politrauma sin TCE grave"),
    MAJOR_BURNS("Gran quemado"),
    SEPTIC_SHOCK("Shock séptico"),
    ACUTE_RESPIRATORY_FAILURE("Insuficiencia respiratoria aguda"),
    COMPLEX_POSTOP("Postoperatorio complejo"),
    OTHER_NEUROLOGICAL("Otros neurológicos"),
    OTHER_MEDICAL("Otros médicos");

    private final String label;

    AdmissionDiagnosis(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
