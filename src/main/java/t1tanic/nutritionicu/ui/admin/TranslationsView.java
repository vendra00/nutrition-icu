package t1tanic.nutritionicu.ui.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import t1tanic.nutritionicu.service.i18n.TranslationAdminService;
import t1tanic.nutritionicu.ui.MainLayout;

/**
 * Admin screen for managing the app's interface text: search and edit translations. Editing is the only
 * change allowed — keys can't be created or deleted here, so an admin can't accidentally remove a key the
 * UI depends on (new keys arrive automatically from the bundles on deploy). Edits go straight to the
 * {@code app_translation} table and refresh the live cache, so users see them on their next page load.
 */
@Route(value = "translations", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class TranslationsView extends VerticalLayout implements HasDynamicTitle {

    private final transient TranslationAdminService service;
    private final Grid<TranslationAdminService.Row> grid = new Grid<>();
    private final TextField search = new TextField();
    private final Checkbox untranslatedOnly = new Checkbox();
    private GridListDataView<TranslationAdminService.Row> dataView;

    @Override
    public String getPageTitle() {
        return getTranslation("admin.tr.title") + " · " + getTranslation("app.title");
    }

    public TranslationsView(TranslationAdminService service) {
        this.service = service;
        setSizeFull();
        setPadding(true);

        Paragraph intro = new Paragraph(getTranslation("admin.tr.intro"));

        search.setPlaceholder(getTranslation("admin.tr.search"));
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidth("28em");
        search.addValueChangeListener(e -> applyFilter());

        untranslatedOnly.setLabel(getTranslation("admin.tr.untranslated"));
        untranslatedOnly.addValueChangeListener(e -> applyFilter());

        HorizontalLayout filterBar = new HorizontalLayout(search, untranslatedOnly);
        filterBar.setAlignItems(Alignment.CENTER);

        grid.addColumn(TranslationAdminService.Row::key)
                .setHeader(getTranslation("admin.tr.col.key")).setAutoWidth(true).setFlexGrow(0).setSortable(true);
        grid.addColumn(TranslationAdminService.Row::english)
                .setHeader(getTranslation("admin.tr.col.en")).setFlexGrow(1);
        grid.addColumn(TranslationAdminService.Row::spanish)
                .setHeader(getTranslation("admin.tr.col.es")).setFlexGrow(1);
        grid.addComponentColumn(this::editButton).setHeader("").setAutoWidth(true).setFlexGrow(0);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        dataView = grid.setItems(service.rows());

        add(new H2(getTranslation("admin.tr.title")), intro, filterBar, grid);
        setFlexGrow(1, grid);
    }

    private Button editButton(TranslationAdminService.Row row) {
        return new Button(getTranslation("common.edit"),
                e -> new TranslationEditorDialog(service, row, this::refresh).open());
    }

    private void refresh() {
        dataView = grid.setItems(service.rows());
        applyFilter();
    }

    private void applyFilter() {
        String term = search.getValue() == null ? "" : search.getValue().strip().toLowerCase();
        boolean gapsOnly = untranslatedOnly.getValue();
        dataView.setFilter(row -> matchesText(row, term) && (!gapsOnly || isUntranslated(row)));
    }

    private static boolean matchesText(TranslationAdminService.Row row, String term) {
        return term.isEmpty()
                || contains(row.key(), term)
                || contains(row.english(), term)
                || contains(row.spanish(), term);
    }

    /** A key whose Spanish value is missing — it falls back to English until a translator fills it in. */
    private static boolean isUntranslated(TranslationAdminService.Row row) {
        return row.spanish() == null || row.spanish().isBlank();
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }
}
