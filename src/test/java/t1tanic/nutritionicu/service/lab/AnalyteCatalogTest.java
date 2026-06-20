package t1tanic.nutritionicu.service.lab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnalyteCatalogTest {

    private final AnalyteCatalog catalog = new AnalyteCatalog();

    @Test
    void mapsSynonymousLabelsToOneCode() {
        // Same analyte, two printed forms across report types -> one canonical code.
        assertThat(catalog.codeFor("Pla-Glucosa")).isEqualTo("GLUCOSE");
        assertThat(catalog.codeFor("vSan-Glucosa")).isEqualTo("GLUCOSE");
        assertThat(catalog.codeFor("Pla-Creatinini")).isEqualTo("CREATININE");
    }

    @Test
    void unknownOrNullLabelReturnsNull() {
        assertThat(catalog.codeFor("Quelcom desconegut")).isNull();
        assertThat(catalog.codeFor(null)).isNull();
    }
}
