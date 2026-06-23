package t1tanic.nutritionicu.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.model.Translation;
import t1tanic.nutritionicu.repo.TranslationRepository;
import t1tanic.nutritionicu.service.i18n.TranslationStore;

/**
 * Seeds the {@code app_translation} table from the bundled {@code translations[_es].properties} (the
 * git-authoritative source) on startup. Insert-if-absent: new keys are added, but existing rows are left
 * untouched so values edited directly in the DB survive restarts. Refreshes the {@link TranslationStore}
 * cache once done. Runs before the data initializers so the UI has its translations from the first request.
 *
 * <p>Set {@code app.i18n.reseed=true} to wipe the table first and rebuild it from the bundles — the way to
 * push edits made in the {@code .properties} files to an already-seeded database.
 */
@Slf4j
@Component
@Order(-100)
public class TranslationSeeder implements ApplicationRunner {

    /** Each properties file mapped to the language tag its values are written in. */
    private static final List<Bundle> BUNDLES = List.of(
            new Bundle("translations.properties", "en"),
            new Bundle("translations_es.properties", "es"));

    private final TranslationRepository repository;
    private final TranslationStore store;
    private final boolean reseed;

    public TranslationSeeder(TranslationRepository repository, TranslationStore store,
                             @Value("${app.i18n.reseed:false}") boolean reseed) {
        this.repository = repository;
        this.store = store;
        this.reseed = reseed;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (reseed) {
            log.info("app.i18n.reseed=true — rebuilding all translations from the bundles");
            repository.deleteAllInBatch();
        }
        int inserted = 0;
        for (Bundle bundle : BUNDLES) {
            inserted += seed(bundle);
        }
        if (inserted > 0) {
            log.info("Seeded {} new translation(s) into the database", inserted);
        }
        store.reload();
    }

    private int seed(Bundle bundle) {
        Properties props = load(bundle.resource());
        Set<String> existing = new HashSet<>();
        for (Translation t : repository.findByLangTag(bundle.langTag())) {
            existing.add(t.getMsgKey());
        }
        List<Translation> toSave = new ArrayList<>();
        for (String key : props.stringPropertyNames()) {
            if (!existing.contains(key)) {
                toSave.add(new Translation(bundle.langTag(), key, props.getProperty(key)));
            }
        }
        repository.saveAll(toSave);
        return toSave.size();
    }

    private static Properties load(String resource) {
        Properties props = new Properties();
        ClassPathResource classpath = new ClassPathResource(resource);
        try (Reader reader = new InputStreamReader(classpath.getInputStream(), StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load translation bundle " + resource, e);
        }
        return props;
    }

    private record Bundle(String resource, String langTag) {
    }
}
