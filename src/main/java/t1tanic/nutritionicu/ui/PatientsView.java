package t1tanic.nutritionicu.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.Locale;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.repo.PatientRepository;
import t1tanic.nutritionicu.service.NutritionService;

/** All patients, with anthropometry and a per-row editor for the doctor. */
@Route(value = "patients", layout = MainLayout.class)
@PageTitle("Patients · ICU Nutrition")
public class PatientsView extends VerticalLayout {

    private final PatientRepository patientRepository;
    private final NutritionService nutritionService;
    private final Grid<Patient> grid = new Grid<>(Patient.class, false);

    public PatientsView(PatientRepository patientRepository, NutritionService nutritionService) {
        this.patientRepository = patientRepository;
        this.nutritionService = nutritionService;
        setSizeFull();
        setPadding(true);
        add(new H2("Patients"));

        grid.addColumn(Patient::getMedicalRecordNumber).setHeader("NHC").setAutoWidth(true);
        grid.addColumn(Patient::getFullName).setHeader("Name").setFlexGrow(2);
        grid.addColumn(Patient::getSex).setHeader("Sex").setAutoWidth(true);
        grid.addColumn(Patient::getBirthDate).setHeader("Born").setAutoWidth(true);
        grid.addColumn(p -> p.isMonitored() ? "Yes" : "No").setHeader("Monitored").setAutoWidth(true);
        grid.addColumn(p -> num(p.getHeightCm())).setHeader("Height (cm)").setAutoWidth(true);
        grid.addColumn(p -> num(p.getCurrentWeightKg())).setHeader("Weight (kg)").setAutoWidth(true);
        grid.addColumn(p -> num(nutritionService.metricsFor(p).bmi())).setHeader("BMI").setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader("").setAutoWidth(true);

        grid.setItems(patientRepository.findAll());
        grid.setSizeFull();
        addAndExpand(grid);
    }

    private Component actions(Patient patient) {
        Button bodyData = new Button("Body data", e ->
                new PatientAnthropometryEditor(patient, nutritionService, this::refresh).open());
        Button weights = new Button("Weights", e -> {
            WeightHistoryDialog dialog = new WeightHistoryDialog(patient, nutritionService);
            dialog.addOpenedChangeListener(ev -> {
                if (!ev.isOpened()) {
                    refresh(); // current weight / BMI may have changed
                }
            });
            dialog.open();
        });
        return new HorizontalLayout(bodyData, weights);
    }

    private void refresh() {
        grid.setItems(patientRepository.findAll());
    }

    private static String num(Double value) {
        return value == null ? "—" : String.format(Locale.US, "%.1f", value);
    }
}
