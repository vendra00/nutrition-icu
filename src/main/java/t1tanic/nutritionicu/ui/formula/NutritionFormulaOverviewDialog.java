package t1tanic.nutritionicu.ui.formula;
import t1tanic.nutritionicu.ui.common.I18n;
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
        setHeaderTitle(getTranslation("foverview.title", p.getName()));
        setWidth("560px");

        VerticalLayout content = new VerticalLayout(
                section(getTranslation("foverview.identity"), identityTable(p)),
                section(getTranslation("foverview.composition"), compositionTable(p)),
                section(getTranslation("foverview.electrolytes"), electrolyteTable(p)),
                section(getTranslation("foverview.other"), otherTable(p)));
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("gap", "var(--lumo-space-m)");
        add(content);

        getFooter().add(new Button(getTranslation("common.close"), e -> close()));
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
        Grid<MetricsTable.Row> grid = MetricsTable.create(I18n.t("foverview.field"));
        grid.setItems(
                new MetricsTable.Row(I18n.t("foverview.code"), p.getCode()),
                new MetricsTable.Row(I18n.t("foverview.name"), p.getName()),
                new MetricsTable.Row(I18n.t("foverview.type"),
                        p.getCategory() == null ? UiFormat.EMPTY : I18n.t("category." + p.getCategory().name())),
                MetricsTable.Row.badge(I18n.t("foverview.source"),
                        I18n.t(p.isBuiltIn() ? "formula.source.builtin" : "formula.source.hospital"),
                        p.isBuiltIn() ? "contrast" : "success"));
        return grid;
    }

    private static Grid<MetricsTable.Row> compositionTable(NutritionProduct p) {
        Grid<MetricsTable.Row> grid = MetricsTable.create(I18n.t("foverview.component"));
        grid.setItems(
                new MetricsTable.Row(I18n.t("foverview.density"), UiFormat.number(p.getDensityKcalPerMl()) + " kcal/ml"),
                new MetricsTable.Row(I18n.t("foverview.protein"), UiFormat.number(p.getProteinPer100ml()) + " g"),
                new MetricsTable.Row(I18n.t("foverview.carbohydrate"), UiFormat.number(p.getCarbsPer100ml()) + " g"),
                new MetricsTable.Row(I18n.t("foverview.fat"), UiFormat.number(p.getFatPer100ml()) + " g"),
                new MetricsTable.Row(I18n.t("foverview.fibre"), UiFormat.number(p.getFiberPer100ml()) + " g"));
        return grid;
    }

    private static Grid<MetricsTable.Row> electrolyteTable(NutritionProduct p) {
        Grid<MetricsTable.Row> grid = MetricsTable.create(I18n.t("foverview.electrolyte"));
        grid.setItems(
                new MetricsTable.Row(I18n.t("energy.reg.na"), UiFormat.number(p.getSodiumMgPer100ml())),
                new MetricsTable.Row(I18n.t("energy.reg.k"), UiFormat.number(p.getPotassiumMgPer100ml())),
                new MetricsTable.Row(I18n.t("energy.reg.cl"), UiFormat.number(p.getChlorideMgPer100ml())),
                new MetricsTable.Row(I18n.t("energy.reg.mg"), UiFormat.number(p.getMagnesiumMgPer100ml())),
                new MetricsTable.Row(I18n.t("energy.reg.ca"), UiFormat.number(p.getCalciumMgPer100ml())),
                new MetricsTable.Row(I18n.t("energy.reg.p"), UiFormat.number(p.getPhosphorusMgPer100ml())));
        return grid;
    }

    private static Grid<MetricsTable.Row> otherTable(NutritionProduct p) {
        Grid<MetricsTable.Row> grid = MetricsTable.create(I18n.t("foverview.field"));
        grid.setItems(
                new MetricsTable.Row(I18n.t("foverview.osmolarity"), nz(p.getOsmolarity())),
                new MetricsTable.Row(I18n.t("foverview.indications"), nz(p.getIndications())));
        return grid;
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? UiFormat.EMPTY : s;
    }
}
