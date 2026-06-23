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
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.LocalDate;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.service.nutrition.NutritionService;
import t1tanic.nutritionicu.service.patient.PatientOverviewService;
import t1tanic.nutritionicu.service.patient.PatientService;

/** All patients, with anthropometry and a per-row editor for the doctor. */
@Route(value = "patients", layout = MainLayout.class)
@PermitAll
public class PatientsView extends VerticalLayout implements HasDynamicTitle {

    @Override
    public String getPageTitle() {
        return getTranslation("patients.title") + " · " + getTranslation("app.title");
    }

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

        Button newPatient = new Button(getTranslation("patients.new"), e ->
                new PatientEditor(null, patientService, this::refresh).open());
        newPatient.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout header = new HorizontalLayout(new H2(getTranslation("patients.title")), newPatient);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        add(header);

        grid.addComponentColumn(this::nhcLink).setHeader(getTranslation("patients.col.nhc")).setAutoWidth(true);
        grid.addColumn(Patient::getFullName).setHeader(getTranslation("patients.col.name")).setFlexGrow(2);
        grid.addColumn(this::sexText).setHeader(getTranslation("patients.col.sex")).setAutoWidth(true);
        grid.addColumn(p -> dateText(p.getBirthDate())).setHeader(getTranslation("patients.col.born")).setAutoWidth(true);
        grid.addColumn(p -> getTranslation(p.isMonitored() ? "common.yes" : "common.no"))
                .setHeader(getTranslation("patients.col.monitored")).setAutoWidth(true);
        grid.addColumn(p -> dateText(p.getAdmissionDate())).setHeader(getTranslation("patients.col.admitted")).setAutoWidth(true);
        grid.addColumn(p -> dateText(p.getDischargeDate())).setHeader(getTranslation("patients.col.discharged")).setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getHeightCm())).setHeader(getTranslation("patients.col.height")).setAutoWidth(true);
        grid.addColumn(p -> UiFormat.number(p.getCurrentWeightKg())).setHeader(getTranslation("patients.col.weight")).setAutoWidth(true);
        grid.addComponentColumn(p -> BmiBadge.ofNullable(nutritionService.metricsFor(p).bmi()))
                .setHeader(getTranslation("patients.col.bmi")).setAutoWidth(true);
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
        Button edit = new Button(getTranslation("common.edit"), e ->
                new PatientEditor(patient, patientService, this::refresh).open());
        Button stay = new Button(getTranslation("patients.stay"), e ->
                new PatientStayDialog(patient, patientService, this::refresh).open());
        return new HorizontalLayout(edit, stay);
    }

    private String sexText(Patient patient) {
        return getTranslation("sex." + (patient.getSex() == null ? "UNKNOWN" : patient.getSex().name()));
    }

    private void refresh() {
        grid.setItems(patientService.findAll());
    }

    private static String dateText(LocalDate date) {
        return UiFormat.date(date);
    }
}
