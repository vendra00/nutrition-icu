package t1tanic.nutritionicu.ui.energy;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import t1tanic.nutritionicu.service.nutrition.EnergyAssessmentService;
import t1tanic.nutritionicu.service.nutrition.HarrisBenedictCalculator;
import t1tanic.nutritionicu.service.nutrition.NutritionFormulary;
import t1tanic.nutritionicu.service.nutrition.NutritionRegimenCalculator;
import t1tanic.nutritionicu.service.nutrition.NutritionService;
import t1tanic.nutritionicu.service.patient.PatientService;

/**
 * Energy-expenditure screen with two sub-tabs: <b>Harris-Benedict</b> (the predictive calculator) and
 * <b>Calorimetry</b> (measured energy expenditure from an indirect-calorimetry device). Both feed the
 * same nutrition administration plan.
 */
@Route(value = "energy", layout = MainLayout.class)
@PermitAll
public class EnergyView extends VerticalLayout implements HasDynamicTitle {

    @Override
    public String getPageTitle() {
        return getTranslation("energy.title") + " · " + getTranslation("app.title");
    }

    public EnergyView(PatientService patientService,
                      NutritionService nutritionService,
                      HarrisBenedictCalculator calculator,
                      EnergyAssessmentService energyService,
                      NutritionRegimenCalculator regimenCalculator,
                      NutritionFormulary formulary) {
        setWidthFull();
        setPadding(true);
        add(new H2(getTranslation("energy.title")));

        HarrisBenedictView harrisBenedict = new HarrisBenedictView(
                patientService, nutritionService, calculator, energyService, regimenCalculator, formulary);
        CalorimetryView calorimetry = new CalorimetryView(
                patientService, nutritionService, energyService, regimenCalculator, formulary);
        EnergyComparisonView comparison = new EnergyComparisonView(patientService, energyService);

        Tab hbTab = new Tab(getTranslation("energy.tab.hb"));
        Tab calorimetryTab = new Tab(getTranslation("energy.tab.calorimetry"));
        Tab comparisonTab = new Tab(getTranslation("energy.tab.comparison"));
        Tabs tabs = new Tabs(hbTab, calorimetryTab, comparisonTab);
        tabs.setWidthFull();

        calorimetry.setVisible(false);
        comparison.setVisible(false);
        tabs.addSelectedChangeListener(e -> {
            Tab selected = e.getSelectedTab();
            harrisBenedict.setVisible(selected == hbTab);
            calorimetry.setVisible(selected == calorimetryTab);
            comparison.setVisible(selected == comparisonTab);
        });

        add(tabs, harrisBenedict, calorimetry, comparison);
    }
}
