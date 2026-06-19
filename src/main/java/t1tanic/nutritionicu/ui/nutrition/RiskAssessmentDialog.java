package t1tanic.nutritionicu.ui.nutrition;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import java.time.LocalDate;
import t1tanic.nutritionicu.dto.NutricScore;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.enums.AdmissionDelayBand;
import t1tanic.nutritionicu.model.enums.AgeBand;
import t1tanic.nutritionicu.model.enums.ApacheBand;
import t1tanic.nutritionicu.model.enums.ComorbidityBand;
import t1tanic.nutritionicu.model.enums.Il6Band;
import t1tanic.nutritionicu.model.enums.SofaBand;
import t1tanic.nutritionicu.service.NutritionService;

/**
 * Banded NUTRIC entry: the doctor selects each severity band from a dropdown; age is
 * derived from the patient. The score and risk class update live.
 */
public class RiskAssessmentDialog extends Dialog {

    private final Select<ApacheBand> apache = new Select<>();
    private final Select<SofaBand> sofa = new Select<>();
    private final Select<ComorbidityBand> comorbidity = new Select<>();
    private final Select<AdmissionDelayBand> admissionDelay = new Select<>();
    private final Select<Il6Band> il6 = new Select<>();
    private final Span result = new Span();

    public RiskAssessmentDialog(Patient patient, NutritionService nutritionService, Runnable onSaved) {
        setHeaderTitle("Nutritional risk · " + patient.getFullName());
        setWidth("440px");

        Integer years = patient.ageOn(LocalDate.now());
        int age = years == null ? 0 : years;
        AgeBand ageBand = AgeBand.fromAge(age);
        add(new Span("Age: " + age + " yrs → " + ageBand.label() + " (" + ageBand.points() + " pt)"));

        configure(apache, "APACHE II", ApacheBand.values(), ApacheBand::label);
        configure(sofa, "SOFA", SofaBand.values(), SofaBand::label);
        configure(comorbidity, "Comorbidities", ComorbidityBand.values(), ComorbidityBand::label);
        configure(admissionDelay, "Days hospital → ICU", AdmissionDelayBand.values(), AdmissionDelayBand::label);

        // Null-safe label: the empty-selection item is rendered with a null value.
        configure(il6, "IL-6 (pg/mL)", Il6Band.values(), band -> band == null ? "Not measured" : band.label());
        il6.setEmptySelectionAllowed(true);
        il6.setEmptySelectionCaption("Not measured");
        il6.setHelperText("Optional — adds the IL-6 component (max 10)");

        FormLayout form = new FormLayout(apache, sofa, comorbidity, admissionDelay, il6);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(form, new Span("NUTRIC"), result);
        recompute(ageBand, nutritionService);

        Button cancel = new Button("Cancel", e -> close());
        Button save = new Button("Save", e -> {
            nutritionService.recordRiskAssessment(patient.getId(), LocalDate.now(),
                    apache.getValue(), sofa.getValue(), comorbidity.getValue(),
                    admissionDelay.getValue(), il6.getValue());
            onSaved.run();
            close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);
    }

    private <T> void configure(Select<T> select, String label, T[] items,
                               com.vaadin.flow.component.ItemLabelGenerator<T> labels) {
        select.setLabel(label);
        select.setItems(items);
        select.setItemLabelGenerator(labels);
        select.addValueChangeListener(e -> recomputeFromListener());
    }

    private NutritionService service;
    private AgeBand ageBand;

    private void recompute(AgeBand ageBand, NutritionService nutritionService) {
        this.ageBand = ageBand;
        this.service = nutritionService;
        recomputeFromListener();
    }

    private void recomputeFromListener() {
        if (service == null) {
            return;
        }
        NutricScore s = service.computeNutric(ageBand, apache.getValue(), sofa.getValue(),
                comorbidity.getValue(), admissionDelay.getValue(), il6.getValue());
        result.setText("Score %d / %d — %s%s".formatted(
                s.score(), s.maxScore(),
                s.highRisk() ? "HIGH risk" : "Low risk",
                s.includesIl6() ? " (with IL-6)" : ""));
    }
}
