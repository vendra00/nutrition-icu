package t1tanic.nutritionicu.config;

import com.vaadin.flow.i18n.I18NProvider;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import t1tanic.nutritionicu.service.i18n.TranslationStore;

/**
 * Resolves UI translation keys from the database-backed {@link TranslationStore} (seeded from
 * {@code translations[_es].properties}). Returns the key itself when a translation is missing, so
 * untranslated strings stay visible rather than blank.
 */
@Component
public class AppI18NProvider implements I18NProvider {

    private static final List<Locale> LOCALES = List.of(LocalePreference.ENGLISH, LocalePreference.SPANISH);

    private final TranslationStore store;

    public AppI18NProvider(TranslationStore store) {
        this.store = store;
    }

    @Override
    public List<Locale> getProvidedLocales() {
        return LOCALES;
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        if (key == null) {
            return "";
        }
        String value = store.find(locale == null ? LocalePreference.ENGLISH : locale, key);
        if (value == null) {
            return key;
        }
        return params == null || params.length == 0 ? value : MessageFormat.format(value, params);
    }
}
