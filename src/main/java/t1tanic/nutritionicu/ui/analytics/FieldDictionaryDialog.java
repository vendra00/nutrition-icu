package t1tanic.nutritionicu.ui.analytics;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.Comparator;
import java.util.List;

/**
 * A plain-language glossary of the lab analytes shown in Analytics: each canonical analyte with its
 * localized name and a short "what it means" description, searchable. Helps staff who don't know a marker.
 */
public class FieldDictionaryDialog extends Dialog {

    private record Entry(String field, String meaning) {
    }

    private final Grid<Entry> grid = new Grid<>();
    private final TextField search = new TextField();
    private GridListDataView<Entry> dataView;

    public FieldDictionaryDialog(List<String> analyteCodes) {
        setHeaderTitle(getTranslation("analytics.dict.title"));
        setWidth("760px");

        Paragraph intro = new Paragraph(getTranslation("analytics.dict.intro"));

        search.setPlaceholder(getTranslation("analytics.dict.search"));
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidthFull();
        search.addValueChangeListener(e -> applyFilter());

        grid.addColumn(Entry::field).setHeader(getTranslation("analytics.dict.col.field"))
                .setAutoWidth(true).setFlexGrow(0).setSortable(true);
        grid.addColumn(Entry::meaning).setHeader(getTranslation("analytics.dict.col.meaning")).setFlexGrow(1);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);
        grid.setAllRowsVisible(true);

        List<Entry> entries = analyteCodes.stream()
                .map(code -> new Entry(getTranslation("analyte.code." + code), getTranslation("analyte.desc." + code)))
                .sorted(Comparator.comparing(Entry::field, String.CASE_INSENSITIVE_ORDER))
                .toList();
        dataView = grid.setItems(entries);

        VerticalLayout content = new VerticalLayout(intro, search, grid);
        content.setPadding(false);
        content.getStyle().set("gap", "var(--lumo-space-s)");
        add(content);
        getFooter().add(new Button(getTranslation("common.close"), e -> close()));
    }

    private void applyFilter() {
        String term = search.getValue() == null ? "" : search.getValue().strip().toLowerCase();
        dataView.setFilter(e -> term.isEmpty() || contains(e.field(), term) || contains(e.meaning(), term));
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }
}
