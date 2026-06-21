package t1tanic.nutritionicu.ui.formula;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import t1tanic.nutritionicu.model.NutritionProduct;
import t1tanic.nutritionicu.model.enums.NutritionCategory;
import t1tanic.nutritionicu.service.nutrition.NutritionFormulary;

/**
 * Add/edit dialog for a nutrition formula. Pass {@code null} to create a new one. Macronutrients are
 * g/100 ml and electrolytes mg/100 ml, matching the catalog. Built-in flag is preserved on edit.
 */
public class NutritionFormulaEditor extends Dialog {

    private final TextField code = new TextField("Code");
    private final TextField name = new TextField("Name");
    private final ComboBox<NutritionCategory> category = new ComboBox<>("Type");
    private final NumberField density = new NumberField("Density (kcal/ml)");
    private final NumberField protein = gramField("Protein (g/100 ml)");
    private final NumberField carbs = gramField("Carbohydrate (g/100 ml)");
    private final NumberField fat = gramField("Fat (g/100 ml)");
    private final NumberField fiber = gramField("Fibre (g/100 ml)");
    private final TextField osmolarity = new TextField("Osmolarity (mOsm/l)");
    private final NumberField sodium = gramField("Sodium (mg/100 ml)");
    private final NumberField potassium = gramField("Potassium (mg/100 ml)");
    private final NumberField chloride = gramField("Chloride (mg/100 ml)");
    private final NumberField magnesium = gramField("Magnesium (mg/100 ml)");
    private final NumberField calcium = gramField("Calcium (mg/100 ml)");
    private final NumberField phosphorus = gramField("Phosphorus (mg/100 ml)");
    private final TextArea indications = new TextArea("Indications");

    public NutritionFormulaEditor(NutritionProduct product, NutritionFormulary formulary, Runnable onSaved) {
        boolean creating = product == null;
        setHeaderTitle(creating ? "New formula" : "Edit · " + product.getName());
        setWidth("700px");

        code.setRequiredIndicatorVisible(true);
        name.setRequiredIndicatorVisible(true);
        category.setItems(NutritionCategory.values());
        category.setItemLabelGenerator(NutritionCategory::label);
        category.setRequiredIndicatorVisible(true);
        density.setRequiredIndicatorVisible(true);
        density.setStep(0.1);
        density.setMin(0);
        indications.setMaxLength(2048);

        if (!creating) {
            prefill(product);
        }

        FormLayout form = new FormLayout(code, name, category, density,
                protein, carbs, fat, fiber, osmolarity,
                sodium, potassium, chloride, magnesium, calcium, phosphorus, indications);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("420px", 2), new FormLayout.ResponsiveStep("640px", 3));
        form.setColspan(indications, 3);
        add(form);

        Button cancel = new Button("Cancel", e -> close());
        Button save = new Button("Save", e -> save(product, formulary, onSaved));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);
    }

    private void prefill(NutritionProduct p) {
        code.setValue(nullToEmpty(p.getCode()));
        name.setValue(nullToEmpty(p.getName()));
        category.setValue(p.getCategory());
        density.setValue(p.getDensityKcalPerMl());
        protein.setValue(p.getProteinPer100ml());
        carbs.setValue(p.getCarbsPer100ml());
        fat.setValue(p.getFatPer100ml());
        fiber.setValue(p.getFiberPer100ml());
        osmolarity.setValue(nullToEmpty(p.getOsmolarity()));
        sodium.setValue(p.getSodiumMgPer100ml());
        potassium.setValue(p.getPotassiumMgPer100ml());
        chloride.setValue(p.getChlorideMgPer100ml());
        magnesium.setValue(p.getMagnesiumMgPer100ml());
        calcium.setValue(p.getCalciumMgPer100ml());
        phosphorus.setValue(p.getPhosphorusMgPer100ml());
        indications.setValue(nullToEmpty(p.getIndications()));
    }

    private void save(NutritionProduct existing, NutritionFormulary formulary, Runnable onSaved) {
        if (isBlank(name.getValue())) {
            error("Name is required");
            return;
        }
        if (category.getValue() == null) {
            error("Type is required");
            return;
        }
        if (density.getValue() == null || density.getValue() <= 0) {
            error("Density (kcal/ml) must be greater than 0");
            return;
        }

        NutritionProduct product = existing != null ? existing : new NutritionProduct();
        product.setCode(code.getValue());
        product.setName(name.getValue().strip());
        product.setCategory(category.getValue());
        product.setDensityKcalPerMl(density.getValue());
        product.setProteinPer100ml(num(protein));
        product.setCarbsPer100ml(num(carbs));
        product.setFatPer100ml(num(fat));
        product.setFiberPer100ml(num(fiber));
        product.setOsmolarity(emptyToNull(osmolarity.getValue()));
        product.setSodiumMgPer100ml(num(sodium));
        product.setPotassiumMgPer100ml(num(potassium));
        product.setChlorideMgPer100ml(num(chloride));
        product.setMagnesiumMgPer100ml(num(magnesium));
        product.setCalciumMgPer100ml(num(calcium));
        product.setPhosphorusMgPer100ml(num(phosphorus));
        product.setIndications(emptyToNull(indications.getValue()));

        try {
            formulary.save(product);
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
            return;
        }
        onSaved.run();
        close();
    }

    private static void error(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private static NumberField gramField(String label) {
        NumberField field = new NumberField(label);
        field.setMin(0);
        return field;
    }

    private static double num(NumberField field) {
        return field.getValue() == null ? 0.0 : field.getValue();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String emptyToNull(String s) {
        if (s == null) {
            return null;
        }
        String stripped = s.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
