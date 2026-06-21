package t1tanic.nutritionicu.ui.formula;
import t1tanic.nutritionicu.ui.common.UiFormat;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import t1tanic.nutritionicu.model.NutritionProduct;
import t1tanic.nutritionicu.service.nutrition.NutritionFormulary;

/** Catalog of nutrition formulas: inspect (click the code), add, edit or remove. */
@Route(value = "nutrition-formula", layout = MainLayout.class)
@PageTitle("Nutrition formula · ICU Nutrition")
@RolesAllowed("ADMIN")
public class NutritionFormulaView extends VerticalLayout {

    private final transient NutritionFormulary formulary;
    private final Grid<NutritionProduct> grid = new Grid<>(NutritionProduct.class, false);

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

        grid.addComponentColumn(this::codeLink).setHeader("Code").setAutoWidth(true);
        grid.addColumn(NutritionProduct::getName).setHeader("Name").setFlexGrow(2);
        grid.addColumn(p -> p.getCategory().label()).setHeader("Type").setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getDensityKcalPerMl()) + " kcal/ml")
                .setHeader("Density").setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getProteinPer100ml())).setHeader("Protein /100ml").setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getCarbsPer100ml())).setHeader("Carbs /100ml").setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getFatPer100ml())).setHeader("Fat /100ml").setAutoWidth(true);
        grid.addColumn(p -> p.isBuiltIn() ? "Built-in" : "Hospital").setHeader("Source").setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader("").setAutoWidth(true);

        grid.setItems(formulary.all());
        grid.setSizeFull();
        addAndExpand(grid);
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
        grid.setItems(formulary.all());
    }
}
