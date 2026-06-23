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
import t1tanic.nutritionicu.service.nutrition.NutritionService;
import t1tanic.nutritionicu.ui.common.I18n;

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
        setHeaderTitle(getTranslation("nd.risk.title", patient.getFullName()));
        setWidth("440px");

        Integer years = patient.ageOn(LocalDate.now());
        int age = years == null ? 0 : years;
        AgeBand ageBand = AgeBand.fromAge(age);
        add(new Span(getTranslation("nd.risk.agesummary",
                String.valueOf(age), ageBand.label(), String.valueOf(ageBand.points()))));

        configure(apache, getTranslation("nd.risk.apache"), ApacheBand.values(), ApacheBand::label);
        configure(sofa, getTranslation("nd.risk.sofa"), SofaBand.values(), SofaBand::label);
        configure(comorbidity, getTranslation("nd.risk.comorbidity"), ComorbidityBand.values(), ComorbidityBand::label);
        configure(admissionDelay, getTranslation("nd.risk.admissiondelay"),
                AdmissionDelayBand.values(), AdmissionDelayBand::label);

        // Null-safe label: the empty-selection item is rendered with a null value.
        configure(il6, getTranslation("nd.risk.il6"), Il6Band.values(),
                band -> band == null ? getTranslation("nd.risk.notmeasured") : band.label());
        il6.setEmptySelectionAllowed(true);
        il6.setEmptySelectionCaption(getTranslation("nd.risk.notmeasured"));
        il6.setHelperText(getTranslation("nd.risk.il6helper"));

        FormLayout form = new FormLayout(apache, sofa, comorbidity, admissionDelay, il6);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(form, new Span(getTranslation("nd.risk.nutric")), result);
        recompute(ageBand, nutritionService);

        Button cancel = new Button(getTranslation("common.cancel"), e -> close());
        Button save = new Button(getTranslation("common.save"), e -> {
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

    private transient NutritionService service;
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
        result.setText(getTranslation("nd.risk.result",
                String.valueOf(s.score()), String.valueOf(s.maxScore()),
                getTranslation(s.highRisk() ? "nd.risk.high" : "nd.risk.low"),
                s.includesIl6() ? getTranslation("nd.risk.withil6") : ""));
    }
}
