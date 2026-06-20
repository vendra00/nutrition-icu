package t1tanic.nutritionicu.ui.energy;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
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
@PageTitle("Energy · ICU Nutrition")
public class EnergyView extends VerticalLayout {

    public EnergyView(PatientService patientService,
                      NutritionService nutritionService,
                      HarrisBenedictCalculator calculator,
                      EnergyAssessmentService energyService,
                      NutritionRegimenCalculator regimenCalculator,
                      NutritionFormulary formulary) {
        setWidthFull();
        setPadding(true);
        add(new H2("Energy expenditure"));

        HarrisBenedictView harrisBenedict = new HarrisBenedictView(
                patientService, nutritionService, calculator, energyService, regimenCalculator, formulary);
        CalorimetryView calorimetry = new CalorimetryView(
                patientService, nutritionService, energyService, regimenCalculator, formulary);

        Tab hbTab = new Tab("Harris-Benedict");
        Tab calorimetryTab = new Tab("Calorimetry");
        Tabs tabs = new Tabs(hbTab, calorimetryTab);
        tabs.setWidthFull();

        calorimetry.setVisible(false);
        tabs.addSelectedChangeListener(e -> {
            boolean hbSelected = e.getSelectedTab() == hbTab;
            harrisBenedict.setVisible(hbSelected);
            calorimetry.setVisible(!hbSelected);
        });

        add(tabs, harrisBenedict, calorimetry);
    }
}
