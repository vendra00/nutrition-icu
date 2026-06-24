package t1tanic.nutritionicu.ui.labreports;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import t1tanic.nutritionicu.dto.LabReportSummary;
import t1tanic.nutritionicu.service.ingestion.LabTestService;
import t1tanic.nutritionicu.service.patient.PatientOverviewService;
import t1tanic.nutritionicu.ui.MainLayout;
import t1tanic.nutritionicu.ui.common.UiFormat;
import t1tanic.nutritionicu.ui.patients.PatientOverviewDialog;

/** The ingested lab reports as a table (newest first), with the upload action to bring in more. */
@Route(value = "lab-reports", layout = MainLayout.class)
@PermitAll
public class LabReportsView extends VerticalLayout implements HasDynamicTitle {

    private static final int RECENT_LIMIT = 500;

    private final transient LabTestService labTestService;
    private final transient PatientOverviewService overviewService;
    private final Grid<LabReportSummary> grid = new Grid<>();

    @Override
    public String getPageTitle() {
        return getTranslation("labreports.title") + " · " + getTranslation("app.title");
    }

    public LabReportsView(LabTestService labTestService, PatientOverviewService overviewService) {
        this.labTestService = labTestService;
        this.overviewService = overviewService;
        setSizeFull();
        setPadding(true);

        Button ingest = new Button(getTranslation("ingest.button"), VaadinIcon.UPLOAD.create(),
                e -> new IngestDialog(labTestService, this::refresh).open());
        ingest.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout header = new HorizontalLayout(new H2(getTranslation("labreports.title")), ingest);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        add(header);

        grid.addComponentColumn(this::nhcLink).setHeader(getTranslation("labreports.col.nhc")).setAutoWidth(true);
        grid.addColumn(LabReportSummary::patientName).setHeader(getTranslation("labreports.col.name")).setFlexGrow(1);
        grid.addColumn(LabReportSummary::orderNumber).setHeader(getTranslation("labreports.col.order")).setAutoWidth(true);
        grid.addColumn(r -> nz(r.department())).setHeader(getTranslation("labreports.col.department")).setAutoWidth(true);
        grid.addColumn(r -> UiFormat.date(r.reportDate())).setHeader(getTranslation("labreports.col.date")).setAutoWidth(true);
        grid.addColumn(LabReportSummary::sectionCount).setHeader(getTranslation("labreports.col.sections")).setAutoWidth(true);
        grid.addColumn(r -> UiFormat.instant(r.ingestedAt())).setHeader(getTranslation("labreports.col.ingested")).setAutoWidth(true);
        grid.addColumn(LabReportSummary::sourceFilename).setHeader(getTranslation("labreports.col.file")).setFlexGrow(1);
        grid.setSizeFull();

        refresh();
        addAndExpand(grid);
    }

    /** The NHC rendered as a link that opens the patient's overview, as on the Patients and Alerts tabs. */
    private Component nhcLink(LabReportSummary report) {
        Button link = new Button(report.patientMrn(),
                e -> new PatientOverviewDialog(report.patientId(), overviewService).open());
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        return link;
    }

    private void refresh() {
        grid.setItems(labTestService.recentReports(RECENT_LIMIT));
    }

    private static String nz(String value) {
        return value == null || value.isBlank() ? UiFormat.EMPTY : value;
    }
}
