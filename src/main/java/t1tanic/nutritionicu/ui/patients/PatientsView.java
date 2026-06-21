package t1tanic.nutritionicu.ui.patients;
import t1tanic.nutritionicu.ui.common.BmiBadge;
import t1tanic.nutritionicu.ui.common.UiFormat;
import t1tanic.nutritionicu.ui.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.LocalDate;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.service.nutrition.NutritionService;
import t1tanic.nutritionicu.service.patient.PatientOverviewService;
import t1tanic.nutritionicu.service.patient.PatientService;

/** All patients, with anthropometry and a per-row editor for the doctor. */
@Route(value = "patients", layout = MainLayout.class)
@PageTitle("Patients · ICU Nutrition")
@PermitAll
public class PatientsView extends VerticalLayout {

    private final transient NutritionService nutritionService;
    private final transient PatientService patientService;
    private final transient PatientOverviewService overviewService;
    private final Grid<Patient> grid = new Grid<>(Patient.class, false);

    public PatientsView(NutritionService nutritionService,
                        PatientService patientService,
                        PatientOverviewService overviewService) {
        this.nutritionService = nutritionService;
        this.patientService = patientService;
        this.overviewService = overviewService;
        setSizeFull();
        setPadding(true);

        Button newPatient = new Button("New patient", e ->
                new PatientEditor(null, patientService, this::refresh).open());
        newPatient.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout header = new HorizontalLayout(new H2("Patients"), newPatient);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        add(header);

        grid.addComponentColumn(this::nhcLink).setHeader("NHC").setAutoWidth(true);
        grid.addColumn(Patient::getFullName).setHeader("Name").setFlexGrow(2);
        grid.addColumn(Patient::getSex).setHeader("Sex").setAutoWidth(true);
        grid.addColumn(p -> dateText(p.getBirthDate())).setHeader("Born").setAutoWidth(true);
        grid.addColumn(p -> p.isMonitored() ? "Yes" : "No").setHeader("Monitored").setAutoWidth(true);
        grid.addColumn(p -> dateText(p.getAdmissionDate())).setHeader("Admitted").setAutoWidth(true);
        grid.addColumn(p -> dateText(p.getDischargeDate())).setHeader("Discharged").setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getHeightCm())).setHeader("Height (cm)").setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getCurrentWeightKg())).setHeader("Weight (kg)").setAutoWidth(true);
        grid.addComponentColumn(p -> BmiBadge.ofNullable(nutritionService.metricsFor(p).bmi()))
                .setHeader("BMI").setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader("").setAutoWidth(true);

        grid.setItems(patientService.findAll());
        grid.setSizeFull();
        addAndExpand(grid);
    }

    /** The NHC rendered as a link that opens the patient's overview dialog. */
    private Component nhcLink(Patient patient) {
        Button link = new Button(patient.getMedicalRecordNumber(), e ->
                new PatientOverviewDialog(patient.getId(), overviewService).open());
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        return link;
    }

    private Component actions(Patient patient) {
        // Demographics here; body-data/weight live in the Nutrition tab, stay dates in the Stay dialog.
        Button edit = new Button("Edit", e ->
                new PatientEditor(patient, patientService, this::refresh).open());
        Button stay = new Button("Stay", e ->
                new PatientStayDialog(patient, patientService, this::refresh).open());
        return new HorizontalLayout(edit, stay);
    }

    private void refresh() {
        grid.setItems(patientService.findAll());
    }

    private static String dateText(LocalDate date) {
        return UiFormat.date(date);
    }
}
