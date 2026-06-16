package t1tanic.nutritionicu.model.enums;

/**
 * Hospital sector / clinical specialty a doctor belongs to.
 * Kept as a controlled vocabulary; extend as new sectors are needed.
 */
public enum Sector {
    ICU,                 // Intensive Care Unit (UCI)
    NEUROLOGY,
    CARDIOLOGY,
    NEPHROLOGY,
    INTERNAL_MEDICINE,
    EMERGENCY,
    SURGERY,
    ONCOLOGY,
    ENDOCRINOLOGY,
    PEDIATRICS,
    GENERAL,
    OTHER
}
