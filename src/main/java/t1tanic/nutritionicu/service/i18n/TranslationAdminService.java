package t1tanic.nutritionicu.service.i18n;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.model.Translation;
import t1tanic.nutritionicu.repo.TranslationRepository;

/**
 * Read/write access to the {@code app_translation} table for the admin Translations screen. Every change
 * refreshes the {@link TranslationStore} cache, so edits made by a non-developer take effect immediately
 * (visible on each view's next render) without a restart or redeploy.
 */
@Service
public class TranslationAdminService {

    /** The two language tags the app provides; the admin screen edits both side by side. */
    public static final String EN = "en";
    public static final String ES = "es";

    private final TranslationRepository repository;
    private final TranslationStore store;

    public TranslationAdminService(TranslationRepository repository, TranslationStore store) {
        this.repository = repository;
        this.store = store;
    }

    /** One editable row per key, carrying the English and Spanish values (either may be {@code null}). */
    public record Row(String key, String english, String spanish) {
    }

    /** Every key with both languages pivoted onto one row, sorted by key. */
    @Transactional(readOnly = true)
    public List<Row> rows() {
        Map<String, String[]> byKey = new TreeMap<>();
        for (Translation t : repository.findAll()) {
            String[] pair = byKey.computeIfAbsent(t.getMsgKey(), k -> new String[2]);
            if (EN.equals(t.getLangTag())) {
                pair[0] = t.getValue();
            } else if (ES.equals(t.getLangTag())) {
                pair[1] = t.getValue();
            }
        }
        return byKey.entrySet().stream()
                .map(e -> new Row(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean exists(String key) {
        return key != null && repository.existsByMsgKey(key.strip());
    }

    /**
     * Upserts a key's English and Spanish values, then reloads the cache. A blank value removes that
     * language's row so the key falls back to English (Spanish) or to the raw key (English).
     */
    @Transactional
    public void save(String key, String english, String spanish) {
        String trimmedKey = key.strip();
        upsert(EN, trimmedKey, english);
        upsert(ES, trimmedKey, spanish);
        store.reload();
    }

    @Transactional
    public void delete(String key) {
        repository.deleteByMsgKey(key.strip());
        store.reload();
    }

    /** Values are stored verbatim — some keys intentionally carry leading/trailing spaces (e.g. " (with IL-6)"). */
    private void upsert(String langTag, String key, String value) {
        Optional<Translation> existing = repository.findByLangTagAndMsgKey(langTag, key);
        if (value == null || value.isBlank()) {
            existing.ifPresent(repository::delete);
            return;
        }
        Translation row = existing.orElseGet(() -> new Translation(langTag, key, value));
        row.setValue(value);
        repository.save(row);
    }
}
