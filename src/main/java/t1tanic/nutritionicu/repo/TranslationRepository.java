package t1tanic.nutritionicu.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import t1tanic.nutritionicu.model.Translation;

public interface TranslationRepository extends JpaRepository<Translation, Long> {

    /** All translations for one language, used to build the in-memory cache and to seed insert-if-absent. */
    List<Translation> findByLangTag(String langTag);
}
