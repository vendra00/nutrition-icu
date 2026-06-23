package t1tanic.nutritionicu.service.i18n;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import t1tanic.nutritionicu.model.Translation;
import t1tanic.nutritionicu.repo.TranslationRepository;

/**
 * In-memory cache of every {@link Translation}, keyed by language tag then message key, so resolving a key
 * is a map lookup rather than a database hit per call. Loaded once at startup and after the seeder runs;
 * call {@link #reload()} whenever the {@code app_translation} table changes to publish the new values.
 */
@Service
public class TranslationStore {

    private static final String DEFAULT_LANG = "en";

    private final TranslationRepository repository;

    /** langTag → (msgKey → value). Replaced wholesale on reload, so reads never see a half-built map. */
    private volatile Map<String, Map<String, String>> cache = Map.of();

    public TranslationStore(TranslationRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void reload() {
        Map<String, Map<String, String>> next = new HashMap<>();
        for (Translation t : repository.findAll()) {
            next.computeIfAbsent(t.getLangTag(), k -> new HashMap<>()).put(t.getMsgKey(), t.getValue());
        }
        this.cache = next;
    }

    /**
     * The value for {@code key} in {@code locale}, or {@code null} when no language in the fallback chain
     * ({@code locale} → its language → {@code en}) has it. The provider turns {@code null} into the key
     * itself so untranslated strings stay visible.
     */
    public String find(Locale locale, String key) {
        for (String tag : candidates(locale)) {
            Map<String, String> byKey = cache.get(tag);
            if (byKey != null) {
                String value = byKey.get(key);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    /** Total number of cached entries across all languages — handy for startup logging. */
    public long size() {
        return cache.values().stream().mapToLong(Map::size).sum();
    }

    private static List<String> candidates(Locale locale) {
        if (locale == null) {
            return List.of(DEFAULT_LANG);
        }
        List<String> tags = new ArrayList<>(3);
        tags.add(locale.toLanguageTag());
        if (!locale.getLanguage().isEmpty()) {
            tags.add(locale.getLanguage());
        }
        tags.add(DEFAULT_LANG);
        return tags.stream().distinct().toList();
    }
}
