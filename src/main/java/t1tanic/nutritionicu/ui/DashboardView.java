package t1tanic.nutritionicu.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import t1tanic.nutritionicu.dto.AlertSummary;
import t1tanic.nutritionicu.model.Doctor;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.repo.DoctorRepository;
import t1tanic.nutritionicu.repo.PatientRepository;
import t1tanic.nutritionicu.service.AlertService;

/**
 * Landing dashboard: active alerts, the monitored-patient cohort, and the doctors
 * on call. Reads through services/repositories — pure Java, no JS.
 */
@Route("")
@PageTitle("ICU Nutrition · Dashboard")
public class DashboardView extends VerticalLayout {

    public DashboardView(AlertService alertService,
                         PatientRepository patientRepository,
                         DoctorRepository doctorRepository) {
        setSizeFull();
        setPadding(true);
        add(new H1("ICU Nutrition Dashboard"));

        add(new H2("Active alerts"));
        add(alertsGrid(alertService));

        add(new H2("Monitored patients"));
        add(monitoredPatientsGrid(patientRepository));

        add(new H2("Doctors"));
        add(doctorsGrid(doctorRepository));
    }

    private Grid<AlertSummary> alertsGrid(AlertService alertService) {
        Grid<AlertSummary> grid = new Grid<>(AlertSummary.class, false);
        grid.addColumn(AlertSummary::severity).setHeader("Severity").setAutoWidth(true);
        grid.addColumn(AlertSummary::status).setHeader("Status").setAutoWidth(true);
        grid.addColumn(AlertSummary::patientMrn).setHeader("Patient (NHC)").setAutoWidth(true);
        grid.addColumn(AlertSummary::sectors).setHeader("Sectors").setAutoWidth(true);
        grid.addColumn(AlertSummary::message).setHeader("Details").setFlexGrow(3);
        grid.addColumn(AlertSummary::createdAt).setHeader("Raised").setAutoWidth(true);
        grid.setItems(alertService.recentAlerts());
        return grid;
    }

    private Grid<Patient> monitoredPatientsGrid(PatientRepository patientRepository) {
        Grid<Patient> grid = new Grid<>(Patient.class, false);
        grid.addColumn(Patient::getMedicalRecordNumber).setHeader("NHC").setAutoWidth(true);
        grid.addColumn(Patient::getFullName).setHeader("Name").setFlexGrow(2);
        grid.addColumn(Patient::getSex).setHeader("Sex").setAutoWidth(true);
        grid.addColumn(Patient::getBirthDate).setHeader("Born").setAutoWidth(true);
        grid.setItems(patientRepository.findByMonitoredTrue());
        return grid;
    }

    private Grid<Doctor> doctorsGrid(DoctorRepository doctorRepository) {
        Grid<Doctor> grid = new Grid<>(Doctor.class, false);
        grid.addColumn(Doctor::getName).setHeader("Name").setFlexGrow(2);
        grid.addColumn(Doctor::getSector).setHeader("Sector").setAutoWidth(true);
        grid.setItems(doctorRepository.findAll());
        return grid;
    }
}
