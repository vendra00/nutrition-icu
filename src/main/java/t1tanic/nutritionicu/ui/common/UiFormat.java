package t1tanic.nutritionicu.ui.common;

import com.vaadin.flow.component.datepicker.DatePicker;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import t1tanic.nutritionicu.model.Patient;

/** Shared display formatting for the views: a missing value renders as an em dash. */
public final class UiFormat {

    /** Placeholder shown for a null/absent value. */
    public static final String EMPTY = "—";

    /** Day-month-year format used for every date shown in the app. */
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US);

    /** Day-month-year with 24-hour time, for timestamps. */
    public static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.US);

    private UiFormat() {
    }

    /** A nullable number to one decimal place, or {@link #EMPTY} when null. */
    public static String number(Double value) {
        return value == null ? EMPTY : String.format(Locale.US, "%.1f", value);
    }

    /** A nullable date as {@code dd-MM-yyyy}, or {@link #EMPTY} when null. */
    public static String date(LocalDate value) {
        return value == null ? EMPTY : DATE.format(value);
    }

    /** A nullable timestamp as {@code dd-MM-yyyy HH:mm}, or {@link #EMPTY} when null. */
    public static String dateTime(LocalDateTime value) {
        return value == null ? EMPTY : DATE_TIME.format(value);
    }

    /** Configures a date picker to display and parse dates as {@code dd-MM-yyyy}. */
    public static void dayMonthYear(DatePicker picker) {
        picker.setI18n(new DatePicker.DatePickerI18n().setDateFormat("dd-MM-yyyy"));
    }

    /** A patient's age today, localized (e.g. "{@code N yrs}" / "{@code N años}"), or {@link #EMPTY}. */
    public static String ageYears(Patient patient) {
        Integer age = patient.ageOn(LocalDate.now());
        return age == null ? EMPTY : I18n.t("common.years", String.valueOf(age));
    }
}
