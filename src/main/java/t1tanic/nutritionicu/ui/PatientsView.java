package t1tanic.nutritionicu.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.repo.PatientRepository;

/** All patients, with their monitoring status. */
@Route(value = "patients", layout = MainLayout.class)
@PageTitle("Patients · ICU Nutrition")
public class PatientsView extends VerticalLayout {

    public PatientsView(PatientRepository patientRepository) {
        setSizeFull();
        setPadding(true);
        add(new H2("Patients"));

        Grid<Patient> grid = new Grid<>(Patient.class, false);
        grid.addColumn(Patient::getMedicalRecordNumber).setHeader("NHC").setAutoWidth(true);
        grid.addColumn(Patient::getFullName).setHeader("Name").setFlexGrow(2);
        grid.addColumn(Patient::getSex).setHeader("Sex").setAutoWidth(true);
        grid.addColumn(Patient::getBirthDate).setHeader("Born").setAutoWidth(true);
        grid.addColumn(patient -> patient.isMonitored() ? "Yes" : "No")
                .setHeader("Monitored").setAutoWidth(true);
        grid.setItems(patientRepository.findAll());
        grid.setSizeFull();
        addAndExpand(grid);
    }
}
