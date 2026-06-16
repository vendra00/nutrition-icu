package t1tanic.nutritionicu.model.enums;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Canonical measurement unit. Each constant carries the printed symbol plus any
 * spelling/spacing variants seen across report formats, so e.g. {@code ng/ml} and
 * {@code ng/mL} both resolve to {@link #NG_PER_ML}. Lookup is case- and
 * whitespace-insensitive via {@link #fromRaw(String)}.
 */
public enum Unit {
    PERCENT("%"),
    MMOL_PER_L("mmol/L"),
    MG_PER_DL("mg/dL"),
    G_PER_DL("g/dL"),
    G_PER_L("g/L"),
    UI_PER_L("UI/L", "U/L", "IU/L"),
    NG_PER_L("ng/L"),
    NG_PER_ML("ng/mL", "ng/ml"),
    PG_PER_ML("pg/mL", "pg/ml"),
    PG("pg"),
    FL("fL"),
    GIGA_PER_L("x10E9/L", "10E9/L", "10^9/L"),
    TERA_PER_L("x10E12/L", "10E12/L"),
    MM_HG("mm Hg", "mmHg"),
    ML_MIN_1_73M2("ml/min/1.73 m2", "mL/min/1.73 m2"),
    RATIO("ràtio", "ratio"),
    SECONDS("seg", "s", "sec");

    private final String symbol;
    private final String[] aliases;

    Unit(String symbol, String... aliases) {
        this.symbol = symbol;
        this.aliases = aliases;
    }

    /** The canonical printed symbol, e.g. "mmol/L". */
    public String getSymbol() {
        return symbol;
    }

    private static final Map<String, Unit> BY_TEXT = new HashMap<>();

    static {
        for (Unit unit : values()) {
            BY_TEXT.put(normalize(unit.symbol), unit);
            for (String alias : unit.aliases) {
                BY_TEXT.put(normalize(alias), unit);
            }
        }
    }

    /** Resolves printed unit text to a canonical unit, or empty if absent/unrecognized. */
    public static Optional<Unit> fromRaw(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_TEXT.get(normalize(raw)));
    }

    private static String normalize(String text) {
        return text.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
