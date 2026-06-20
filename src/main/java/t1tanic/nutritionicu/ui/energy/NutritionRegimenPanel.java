package t1tanic.nutritionicu.ui.energy;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.ArrayList;
import java.util.List;
import t1tanic.nutritionicu.dto.EnergyExpenditureResult;
import t1tanic.nutritionicu.dto.NutritionRegimen;
import t1tanic.nutritionicu.model.NutritionProduct;
import t1tanic.nutritionicu.model.enums.NutritionCategory;
import t1tanic.nutritionicu.service.nutrition.NutritionFormulary;
import t1tanic.nutritionicu.service.nutrition.NutritionRegimenCalculator;
import t1tanic.nutritionicu.ui.common.UiFormat;

/**
 * The "Choose nutrition" step shared by the Energy tabs: pick a formula and see its 24-hour
 * administration plan (infusion, macros, electrolytes, protein target) for a given energy target.
 * Drive it with {@link #update(EnergyExpenditureResult, double)} when an energy result is available,
 * or {@link #clear()} when not — the source of the energy (Harris-Benedict or calorimetry) is irrelevant.
 */
public class NutritionRegimenPanel extends Composite<Details> {

    private final transient NutritionRegimenCalculator regimenCalculator;
    private final transient NutritionFormulary formulary;

    private final RadioButtonGroup<NutritionCategory> categoryBox = new RadioButtonGroup<>("Type");
    private final ComboBox<NutritionProduct> productBox = new ComboBox<>("Nutrition formula");
    private final Span regimenPrompt = new Span();
    private final Grid<SummaryRow> summaryGrid = new Grid<>();
    private final Grid<MacroRow> macroGrid = new Grid<>();
    private final Grid<ElectrolyteRow> electrolyteGrid = new Grid<>();
    private final HorizontalLayout tables = new HorizontalLayout();

    private EnergyExpenditureResult energy;
    private double actualWeightKg;
    private String noEnergyPrompt = "Provide an energy target first.";

    public NutritionRegimenPanel(NutritionRegimenCalculator regimenCalculator, NutritionFormulary formulary) {
        this.regimenCalculator = regimenCalculator;
        this.formulary = formulary;

        categoryBox.setItems(NutritionCategory.values());
        categoryBox.setItemLabelGenerator(NutritionCategory::label);
        categoryBox.setValue(NutritionCategory.ENTERAL);
        categoryBox.addValueChangeListener(e -> applyCategoryFilter());
        categoryBox.getStyle().set("margin-right", "var(--lumo-space-xl)");
        productBox.setWidth("28em");
        productBox.setItemLabelGenerator(NutritionProduct::getName);
        productBox.addValueChangeListener(e -> renderRegimen());
        applyCategoryFilter();

        configureMacroGrid();
        configureElectrolyteGrid();
        tables.add(new Div(sectionLabel("Delivered over 24 h"), macroGrid),
                new Div(sectionLabel("Electrolytes (24 h)"), electrolyteGrid));
        tables.getStyle().set("flex-wrap", "wrap").set("gap", "var(--lumo-space-l)");

        regimenPrompt.addClassName(LumoUtility.TextColor.SECONDARY);

        summaryGrid.addColumn(SummaryRow::item).setHeader("Plan").setAutoWidth(true).setFlexGrow(0);
        summaryGrid.addComponentColumn(SummaryRow::valueComponent).setHeader("Value")
                .setAutoWidth(true).setFlexGrow(1);
        summaryGrid.setAllRowsVisible(true);
        summaryGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        summaryGrid.setWidthFull();
        summaryGrid.setMaxWidth("52em");

        VerticalLayout regimen = new VerticalLayout(regimenPrompt, summaryGrid, tables);
        regimen.setPadding(false);
        regimen.setSpacing(false);
        regimen.getStyle().set("gap", "var(--lumo-space-m)");

        HorizontalLayout selectors = new HorizontalLayout(categoryBox, productBox);
        selectors.setPadding(false);
        selectors.setSpacing(true);
        selectors.getThemeList().add("spacing-xl");
        selectors.setAlignItems(FlexComponent.Alignment.START);

        VerticalLayout content = new VerticalLayout(selectors, regimen);
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("gap", "var(--lumo-space-l)").set("padding-top", "var(--lumo-space-s)");

        getContent().setSummaryText("Choose nutrition");
        getContent().add(content);
        getContent().setOpened(true);
        renderRegimen();
    }

    /** Sets the prompt shown when no energy target is available (tab-specific wording). */
    public void setNoEnergyPrompt(String prompt) {
        this.noEnergyPrompt = prompt;
        if (energy == null) {
            renderRegimen();
        }
    }

    /** Drives the plan from an energy result and the patient's actual weight. */
    public void update(EnergyExpenditureResult energy, double actualWeightKg) {
        this.energy = energy;
        this.actualWeightKg = actualWeightKg;
        renderRegimen();
    }

    /** Clears the plan (no energy target available). */
    public void clear() {
        this.energy = null;
        renderRegimen();
    }

    /** Restricts the formula dropdown to the selected category, clearing a now-hidden selection. */
    private void applyCategoryFilter() {
        NutritionCategory category = categoryBox.getValue();
        productBox.clear();
        productBox.setItems(formulary.all().stream()
                .filter(p -> p.getCategory() == category)
                .toList());
    }

    private void renderRegimen() {
        NutritionProduct product = productBox.getValue();
        if (energy == null || product == null) {
            regimenPrompt.setText(energy == null
                    ? noEnergyPrompt
                    : "Select a formula to see the administration plan.");
            setRegimenContentVisible(false);
            return;
        }
        setRegimenContentVisible(true);

        NutritionRegimen plan = regimenCalculator.calculate(energy, actualWeightKg, product);
        macroGrid.setItems(macroRows(plan));
        electrolyteGrid.setItems(electrolyteRows(plan.electrolytes()));
        summaryGrid.setItems(summaryRows(plan, product));
    }

    /** Shows the prompt OR the regimen content, never both. */
    private void setRegimenContentVisible(boolean visible) {
        regimenPrompt.setVisible(!visible);
        summaryGrid.setVisible(visible);
        tables.setVisible(visible);
    }

    private static List<SummaryRow> summaryRows(NutritionRegimen plan, NutritionProduct product) {
        String osm = product.getOsmolarity() == null ? "" : ", " + product.getOsmolarity() + " mOsm/l";
        List<SummaryRow> rows = new ArrayList<>(List.of(
                new SummaryRow("Infusion", "%d ml/h (%d ml/day, %s kcal/ml%s)".formatted(
                        plan.infusionMlPerHour(), plan.dailyVolumeMl(),
                        UiFormat.number(product.getDensityKcalPerMl()), osm), false),
                new SummaryRow("Protein target", "%s g/day (%s g/kg of %s)".formatted(
                        UiFormat.number(plan.proteinTargetG()),
                        UiFormat.number(plan.proteinTargetPerKg()), plan.proteinBasis()), false)));
        if (plan.proteinDeficitG() > 0) {
            rows.add(new SummaryRow("Protein deficit vs target",
                    "%d g/day".formatted(plan.proteinDeficitG()), true));
        }
        if (product.getIndications() != null && !product.getIndications().isBlank()) {
            rows.add(new SummaryRow("Indications", product.getIndications(), false));
        }
        return rows;
    }

    private static List<MacroRow> macroRows(NutritionRegimen plan) {
        List<MacroRow> rows = new ArrayList<>(List.of(
                new MacroRow("Protein", plan.proteinG() + " g", plan.proteinPercent() + "%"),
                new MacroRow("Carbohydrate", plan.carbG() + " g", plan.carbPercent() + "%"),
                new MacroRow("Fat", plan.fatG() + " g", plan.fatPercent() + "%"),
                new MacroRow("Nitrogen", UiFormat.number(plan.nitrogenG()) + " g", "")));
        if (plan.fiberApplicable()) {
            rows.add(new MacroRow("Fibre", UiFormat.number(plan.fiberG()) + " g", ""));
        }
        return rows;
    }

    private static List<ElectrolyteRow> electrolyteRows(NutritionRegimen.Electrolytes el) {
        return List.of(
                new ElectrolyteRow("Sodium (Na)", UiFormat.number(el.sodiumG())),
                new ElectrolyteRow("Potassium (K)", UiFormat.number(el.potassiumG())),
                new ElectrolyteRow("Chloride (Cl)", UiFormat.number(el.chlorideG())),
                new ElectrolyteRow("Calcium (Ca)", UiFormat.number(el.calciumG())),
                new ElectrolyteRow("Magnesium (Mg)", UiFormat.number(el.magnesiumG())),
                new ElectrolyteRow("Phosphorus (P)", UiFormat.number(el.phosphorusG())));
    }

    private void configureMacroGrid() {
        macroGrid.addColumn(MacroRow::nutrient).setHeader("Nutrient").setAutoWidth(true).setFlexGrow(1);
        macroGrid.addColumn(MacroRow::amount).setHeader("Amount / 24 h")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        macroGrid.addColumn(MacroRow::share).setHeader("% kcal")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        macroGrid.setAllRowsVisible(true);
        macroGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        macroGrid.setWidth("28em");
    }

    private void configureElectrolyteGrid() {
        electrolyteGrid.addColumn(ElectrolyteRow::name).setHeader("Electrolyte")
                .setAutoWidth(true).setFlexGrow(1);
        electrolyteGrid.addColumn(ElectrolyteRow::amount).setHeader("g / 24 h")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        electrolyteGrid.setAllRowsVisible(true);
        electrolyteGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        electrolyteGrid.setWidth("22em");
    }

    private static Span sectionLabel(String text) {
        Span span = new Span(text);
        span.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        span.getStyle().set("display", "block").set("margin-bottom", "var(--lumo-space-xs)");
        return span;
    }

    /** A regimen-summary row; {@code warn} renders the value in the error colour (e.g. protein deficit). */
    private record SummaryRow(String item, String value, boolean warn) {

        Span valueComponent() {
            Span span = new Span(value);
            span.getStyle().set("white-space", "normal");
            if (warn) {
                span.getStyle().set("color", "var(--lumo-error-text-color)").set("font-weight", "500");
            }
            return span;
        }
    }

    private record MacroRow(String nutrient, String amount, String share) {
    }

    private record ElectrolyteRow(String name, String amount) {
    }
}
