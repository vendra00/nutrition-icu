package t1tanic.nutritionicu.dto;

/** A minimal patient reference (id for opening the overview, NHC and name for display). */
public record PatientRef(Long id, String mrn, String name) {
}
