package t1tanic.nutritionicu.ui.formula;
import t1tanic.nutritionicu.ui.common.MetricsTable;
import t1tanic.nutritionicu.ui.common.UiFormat;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import t1tanic.nutritionicu.model.NutritionProduct;

/** Read-only full data of a nutrition formula, opened from the code link in the Nutrition formula tab. */
public class NutritionFormulaOverviewDialog extends Dialog {

    public NutritionFormulaOverviewDialog(NutritionProduct p) {
        setHeaderTitle("Formula · " + p.getName());
        setWidth("560px");

        VerticalLayout content = new VerticalLayout(
                section("Identity", identityTable(p)),
                section("Composition (per 100 ml)", compositionTable(p)),
                section("Electrolytes (mg / 100 ml)", electrolyteTable(p)),
                section("Other", otherTable(p)));
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("gap", "var(--lumo-space-m)");
        add(content);

        getFooter().add(new Button("Close", e -> close()));
    }

    private static VerticalLayout section(String title, Grid<MetricsTable.Row> table) {
        H4 heading = new H4(title);
        heading.getStyle().set("margin", "0");
        table.setWidthFull();
        VerticalLayout box = new VerticalLayout(heading, table);
        box.setPadding(false);
        box.setSpacing(false);
        box.getStyle().set("gap", "var(--lumo-space-xs)");
        return box;
    }

    private static Grid<MetricsTable.Row> identityTable(NutritionProduct p) {
        Grid<MetricsTable.Row> grid = MetricsTable.create("Field");
        grid.setItems(
                new MetricsTable.Row("Code", p.getCode()),
                new MetricsTable.Row("Name", p.getName()),
                new MetricsTable.Row("Type", p.getCategory() == null ? UiFormat.EMPTY : p.getCategory().label()),
                MetricsTable.Row.badge("Source", p.isBuiltIn() ? "Built-in" : "Hospital",
                        p.isBuiltIn() ? "contrast" : "success"));
        return grid;
    }

    private static Grid<MetricsTable.Row> compositionTable(NutritionProduct p) {
        Grid<MetricsTable.Row> grid = MetricsTable.create("Component");
        grid.setItems(
                new MetricsTable.Row("Density", UiFormat.number(p.getDensityKcalPerMl()) + " kcal/ml"),
                new MetricsTable.Row("Protein", UiFormat.number(p.getProteinPer100ml()) + " g"),
                new MetricsTable.Row("Carbohydrate", UiFormat.number(p.getCarbsPer100ml()) + " g"),
                new MetricsTable.Row("Fat", UiFormat.number(p.getFatPer100ml()) + " g"),
                new MetricsTable.Row("Fibre", UiFormat.number(p.getFiberPer100ml()) + " g"));
        return grid;
    }

    private static Grid<MetricsTable.Row> electrolyteTable(NutritionProduct p) {
        Grid<MetricsTable.Row> grid = MetricsTable.create("Electrolyte");
        grid.setItems(
                new MetricsTable.Row("Sodium (Na)", UiFormat.number(p.getSodiumMgPer100ml())),
                new MetricsTable.Row("Potassium (K)", UiFormat.number(p.getPotassiumMgPer100ml())),
                new MetricsTable.Row("Chloride (Cl)", UiFormat.number(p.getChlorideMgPer100ml())),
                new MetricsTable.Row("Magnesium (Mg)", UiFormat.number(p.getMagnesiumMgPer100ml())),
                new MetricsTable.Row("Calcium (Ca)", UiFormat.number(p.getCalciumMgPer100ml())),
                new MetricsTable.Row("Phosphorus (P)", UiFormat.number(p.getPhosphorusMgPer100ml())));
        return grid;
    }

    private static Grid<MetricsTable.Row> otherTable(NutritionProduct p) {
        Grid<MetricsTable.Row> grid = MetricsTable.create("Field");
        grid.setItems(
                new MetricsTable.Row("Osmolarity", nz(p.getOsmolarity())),
                new MetricsTable.Row("Indications", nz(p.getIndications())));
        return grid;
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? UiFormat.EMPTY : s;
    }
}
