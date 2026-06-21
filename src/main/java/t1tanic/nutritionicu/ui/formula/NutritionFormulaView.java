package t1tanic.nutritionicu.ui.formula;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import t1tanic.nutritionicu.model.NutritionProduct;
import t1tanic.nutritionicu.model.enums.NutritionCategory;
import t1tanic.nutritionicu.service.nutrition.NutritionFormulary;

/** Catalog of nutrition formulas: inspect (click the code), add, edit or remove. */
@Route(value = "nutrition-formula", layout = MainLayout.class)
@PageTitle("Nutrition formula · ICU Nutrition")
@RolesAllowed("ADMIN")
public class NutritionFormulaView extends VerticalLayout {

    private static final String BUILT_IN = "Built-in";
    private static final String HOSPITAL = "Hospital";

    private final transient NutritionFormulary formulary;
    private final Grid<NutritionProduct> grid = new Grid<>(NutritionProduct.class, false);

    private final TextField codeFilter = filterField("Code…");
    private final TextField nameFilter = filterField("Name…");
    private final ComboBox<NutritionCategory> typeFilter = new ComboBox<>();
    private final ComboBox<String> sourceFilter = new ComboBox<>();

    private transient GridListDataView<NutritionProduct> dataView;

    public NutritionFormulaView(NutritionFormulary formulary) {
        this.formulary = formulary;
        setSizeFull();
        setPadding(true);

        Button newFormula = new Button("New formula", e ->
                new NutritionFormulaEditor(null, formulary, this::refresh).open());
        newFormula.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout header = new HorizontalLayout(new H2("Nutrition formulas"), newFormula);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        add(header);

        Grid.Column<NutritionProduct> codeCol = grid.addComponentColumn(this::codeLink)
                .setHeader("Code").setAutoWidth(true);
        Grid.Column<NutritionProduct> nameCol = grid.addColumn(NutritionProduct::getName)
                .setHeader("Name").setFlexGrow(2);
        Grid.Column<NutritionProduct> typeCol = grid.addColumn(p -> p.getCategory().label())
                .setHeader("Type").setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getDensityKcalPerMl()) + " kcal/ml")
                .setHeader("Density").setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getProteinPer100ml())).setHeader("Protein /100ml").setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getCarbsPer100ml())).setHeader("Carbs /100ml").setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getFatPer100ml())).setHeader("Fat /100ml").setAutoWidth(true);
        Grid.Column<NutritionProduct> sourceCol = grid.addColumn(p -> p.isBuiltIn() ? BUILT_IN : HOSPITAL)
                .setHeader("Source").setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader("").setAutoWidth(true);

        dataView = grid.setItems(formulary.all());
        grid.setSizeFull();

        configureFilters(codeCol, nameCol, typeCol, sourceCol);
        addAndExpand(grid);
    }

    private void configureFilters(Grid.Column<NutritionProduct> codeCol, Grid.Column<NutritionProduct> nameCol,
                                  Grid.Column<NutritionProduct> typeCol, Grid.Column<NutritionProduct> sourceCol) {
        typeFilter.setItems(NutritionCategory.values());
        typeFilter.setItemLabelGenerator(NutritionCategory::label);
        typeFilter.setPlaceholder("All");
        typeFilter.setClearButtonVisible(true);
        typeFilter.setWidthFull();

        sourceFilter.setItems(BUILT_IN, HOSPITAL);
        sourceFilter.setPlaceholder("All");
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
        String source = sourceFilter.getValue();
        return source == null || product.isBuiltIn() == BUILT_IN.equals(source);
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
        Button edit = new Button("Edit", e ->
                new NutritionFormulaEditor(product, formulary, this::refresh).open());
        Button delete = new Button("Delete", e -> confirmDelete(product));
        return new HorizontalLayout(edit, delete);
    }

    private void confirmDelete(NutritionProduct product) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete formula");
        confirm.setText("Delete \"%s\" (%s)?%s".formatted(product.getName(), product.getCode(),
                product.isBuiltIn() ? " This built-in formula will reappear on the next restart." : ""));
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
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
