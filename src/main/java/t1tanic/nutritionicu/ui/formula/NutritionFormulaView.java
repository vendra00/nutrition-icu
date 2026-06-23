package t1tanic.nutritionicu.ui.formula;
import t1tanic.nutritionicu.ui.common.I18n;
import t1tanic.nutritionicu.ui.common.UiFormat;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import t1tanic.nutritionicu.model.NutritionProduct;
import t1tanic.nutritionicu.model.enums.NutritionCategory;
import t1tanic.nutritionicu.service.nutrition.NutritionFormulary;

/** Catalog of nutrition formulas: inspect (click the code), add, edit or remove. */
@Route(value = "nutrition-formula", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class NutritionFormulaView extends VerticalLayout implements HasDynamicTitle {

    @Override
    public String getPageTitle() {
        return getTranslation("formula.title") + " · " + getTranslation("app.title");
    }

    private final transient NutritionFormulary formulary;
    private final Grid<NutritionProduct> grid = new Grid<>(NutritionProduct.class, false);

    private final TextField codeFilter = filterField(I18n.t("formula.filter.code"));
    private final TextField nameFilter = filterField(I18n.t("formula.filter.name"));
    private final ComboBox<NutritionCategory> typeFilter = new ComboBox<>();
    private final ComboBox<Boolean> sourceFilter = new ComboBox<>();

    private transient GridListDataView<NutritionProduct> dataView;

    public NutritionFormulaView(NutritionFormulary formulary) {
        this.formulary = formulary;
        setSizeFull();
        setPadding(true);

        Button newFormula = new Button(getTranslation("formula.new"), e ->
                new NutritionFormulaEditor(null, formulary, this::refresh).open());
        newFormula.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout header = new HorizontalLayout(new H2(getTranslation("formula.title")), newFormula);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        add(header);

        Grid.Column<NutritionProduct> codeCol = grid.addComponentColumn(this::codeLink)
                .setHeader(getTranslation("formula.col.code")).setAutoWidth(true);
        Grid.Column<NutritionProduct> nameCol = grid.addColumn(NutritionProduct::getName)
                .setHeader(getTranslation("formula.col.name")).setFlexGrow(2);
        Grid.Column<NutritionProduct> typeCol = grid.addColumn(p -> getTranslation("category." + p.getCategory().name()))
                .setHeader(getTranslation("formula.col.type")).setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getDensityKcalPerMl()) + " kcal/ml")
                .setHeader(getTranslation("formula.col.density")).setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getProteinPer100ml())).setHeader(getTranslation("formula.col.protein")).setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getCarbsPer100ml())).setHeader(getTranslation("formula.col.carbs")).setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getFatPer100ml())).setHeader(getTranslation("formula.col.fat")).setAutoWidth(true);
        Grid.Column<NutritionProduct> sourceCol = grid.addColumn(this::sourceText)
                .setHeader(getTranslation("formula.col.source")).setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader("").setAutoWidth(true);

        dataView = grid.setItems(formulary.all());
        grid.setSizeFull();

        configureFilters(codeCol, nameCol, typeCol, sourceCol);
        addAndExpand(grid);
    }

    private void configureFilters(Grid.Column<NutritionProduct> codeCol, Grid.Column<NutritionProduct> nameCol,
                                  Grid.Column<NutritionProduct> typeCol, Grid.Column<NutritionProduct> sourceCol) {
        typeFilter.setItems(NutritionCategory.values());
        typeFilter.setItemLabelGenerator(c -> getTranslation("category." + c.name()));
        typeFilter.setPlaceholder(getTranslation("filter.all"));
        typeFilter.setClearButtonVisible(true);
        typeFilter.setWidthFull();

        sourceFilter.setItems(Boolean.TRUE, Boolean.FALSE);
        sourceFilter.setItemLabelGenerator(b -> getTranslation(b ? "formula.source.builtin" : "formula.source.hospital"));
        sourceFilter.setPlaceholder(getTranslation("filter.all"));
        sourceFilter.setClearButtonVisible(true);
        sourceFilter.setWidthFull();

        codeFilter.addValueChangeListener(e -> applyFilter());
        nameFilter.addValueChangeListener(e -> applyFilter());
        typeFilter.addValueChangeListener(e -> applyFilter());
        sourceFilter.addValueChangeListener(e -> applyFilter());

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(codeCol).setComponent(codeFilter);
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(typeCol).setComponent(typeFilter);
        filterRow.getCell(sourceCol).setComponent(sourceFilter);
    }

    private void applyFilter() {
        dataView.setFilter(this::matches);
    }

    private boolean matches(NutritionProduct product) {
        if (!containsIgnoreCase(product.getCode(), codeFilter.getValue())) {
            return false;
        }
        if (!containsIgnoreCase(product.getName(), nameFilter.getValue())) {
            return false;
        }
        if (typeFilter.getValue() != null && product.getCategory() != typeFilter.getValue()) {
            return false;
        }
        Boolean builtIn = sourceFilter.getValue();
        return builtIn == null || product.isBuiltIn() == builtIn;
    }

    private String sourceText(NutritionProduct product) {
        return getTranslation(product.isBuiltIn() ? "formula.source.builtin" : "formula.source.hospital");
    }

    private static boolean containsIgnoreCase(String value, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        return value != null && value.toLowerCase().contains(filter.strip().toLowerCase());
    }

    private static TextField filterField(String placeholder) {
        TextField field = new TextField();
        field.setPlaceholder(placeholder);
        field.setClearButtonVisible(true);
        field.setValueChangeMode(ValueChangeMode.LAZY);
        field.setWidthFull();
        return field;
    }

    /** The code rendered as a link that opens the formula's full data. */
    private Component codeLink(NutritionProduct product) {
        Button link = new Button(product.getCode(), e -> new NutritionFormulaOverviewDialog(product).open());
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        return link;
    }

    private Component actions(NutritionProduct product) {
        Button edit = new Button(getTranslation("common.edit"), e ->
                new NutritionFormulaEditor(product, formulary, this::refresh).open());
        Button delete = new Button(getTranslation("common.delete"), e -> confirmDelete(product));
        return new HorizontalLayout(edit, delete);
    }

    private void confirmDelete(NutritionProduct product) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(getTranslation("formula.delete.header"));
        String note = product.isBuiltIn() ? getTranslation("formula.delete.builtinnote") : "";
        confirm.setText(getTranslation("formula.delete.text", product.getName(), product.getCode(), note));
        confirm.setCancelable(true);
        confirm.setConfirmText(getTranslation("common.delete"));
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> {
            formulary.delete(product.getId());
            refresh();
        });
        confirm.open();
    }

    private void refresh() {
        dataView = grid.setItems(formulary.all());
        applyFilter();
    }
}
