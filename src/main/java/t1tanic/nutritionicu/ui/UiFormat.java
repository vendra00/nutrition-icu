package t1tanic.nutritionicu.ui;

import java.time.LocalDate;
import java.util.Locale;
import t1tanic.nutritionicu.model.Patient;

/** Shared display formatting for the views: a missing value renders as an em dash. */
final class UiFormat {

    /** Placeholder shown for a null/absent value. */
    static final String EMPTY = "—";

    private UiFormat() {
    }

    /** A nullable number to one decimal place, or {@link #EMPTY} when null. */
    static String number(Double value) {
        return value == null ? EMPTY : String.format(Locale.US, "%.1f", value);
    }

    /** A patient's age today as "{@code N yrs}", or {@link #EMPTY} when the birth date is unknown. */
    static String ageYears(Patient patient) {
        Integer age = patient.ageOn(LocalDate.now());
        return age == null ? EMPTY : age + " yrs";
    }
}
