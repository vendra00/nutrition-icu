package t1tanic.nutritionicu.ui.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
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
 * Admin screen for managing the app's interface text: search, edit, add and delete translations. Changes go
 * straight to the {@code app_translation} table and refresh the live cache, so non-developers can adjust
 * wording on the fly — users see it on their next page load.
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

        Button add = new Button(getTranslation("admin.tr.new"), e -> openEditor(null));
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout header = new HorizontalLayout(new H2(getTranslation("admin.tr.title")), add);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

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
        grid.addComponentColumn(this::actions).setHeader("").setAutoWidth(true).setFlexGrow(0);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        dataView = grid.setItems(service.rows());

        add(header, intro, search, grid);
        setFlexGrow(1, grid);
    }

    private Component actions(TranslationAdminService.Row row) {
        Button edit = new Button(getTranslation("common.edit"), e -> openEditor(row));
        Button delete = new Button(getTranslation("common.delete"), e -> confirmDelete(row));
        delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        HorizontalLayout actions = new HorizontalLayout(edit, delete);
        actions.setSpacing(false);
        return actions;
    }

    private void openEditor(TranslationAdminService.Row row) {
        new TranslationEditorDialog(service, row, this::refresh).open();
    }

    private void confirmDelete(TranslationAdminService.Row row) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(getTranslation("admin.tr.delete.header"));
        confirm.setText(getTranslation("admin.tr.delete.text", row.key()));
        confirm.setCancelable(true);
        confirm.setConfirmText(getTranslation("common.delete"));
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> {
            service.delete(row.key());
            Notification.show(getTranslation("admin.tr.deleted", row.key()), 3000, Notification.Position.BOTTOM_START);
            refresh();
        });
        confirm.open();
    }

    private void refresh() {
        dataView = grid.setItems(service.rows());
        applyFilter();
    }

    private void applyFilter() {
        String term = search.getValue() == null ? "" : search.getValue().strip().toLowerCase();
        dataView.setFilter(row -> term.isEmpty()
                || contains(row.key(), term)
                || contains(row.english(), term)
                || contains(row.spanish(), term));
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }
}
