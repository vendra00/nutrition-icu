package t1tanic.nutritionicu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One UI translation: the {@code value} for a {@code msgKey} in a given {@code langTag} (e.g. {@code en},
 * {@code es}). The runtime source of truth for {@link t1tanic.nutritionicu.config.AppI18NProvider}; seeded
 * from the bundled {@code translations[_es].properties} on startup and editable directly in the DB.
 */
@Entity
@Table(name = "app_translation",
        uniqueConstraints = @UniqueConstraint(name = "uk_translation_lang_key", columnNames = {"lang_tag", "msg_key"}),
        indexes = @Index(name = "idx_translation_lang", columnList = "lang_tag"))
@Getter
@Setter
@NoArgsConstructor
public class Translation extends BaseEntity {

    /** IETF language tag the value is written in, e.g. {@code en} or {@code es}. */
    @Column(name = "lang_tag", nullable = false, length = 16)
    private String langTag;

    /** The translation key, e.g. {@code alerts.title} or {@code analyte.code.SODIUM}. */
    @Column(name = "msg_key", nullable = false, length = 200)
    private String msgKey;

    /**
     * The translated text; may contain {@code MessageFormat} placeholders like {@code {0}}. The column name
     * is backtick-quoted because {@code value} is a reserved word in some databases (e.g. H2); Hibernate
     * emits it quoted (`"value"`), which still maps to the existing lowercase {@code value} column in Postgres.
     */
    @Column(name = "`value`", nullable = false, columnDefinition = "text")
    private String value;

    public Translation(String langTag, String msgKey, String value) {
        this.langTag = langTag;
        this.msgKey = msgKey;
        this.value = value;
    }
}
