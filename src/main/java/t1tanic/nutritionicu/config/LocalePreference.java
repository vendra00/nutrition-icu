package t1tanic.nutritionicu.config;

import com.vaadin.flow.server.VaadinSession;
import java.util.Locale;

/** The supported UI locales and the per-session language preference. */
public final class LocalePreference {

    public static final Locale ENGLISH = Locale.ENGLISH;
    public static final Locale SPANISH = Locale.forLanguageTag("es-ES");

    private static final String ATTRIBUTE = "app.locale";

    private LocalePreference() {
    }

    public static Locale get(VaadinSession session) {
        Object value = session == null ? null : session.getAttribute(ATTRIBUTE);
        return value instanceof Locale locale ? locale : ENGLISH;
    }

    public static void set(VaadinSession session, Locale locale) {
        if (session != null) {
            session.setAttribute(ATTRIBUTE, locale);
        }
    }
}
