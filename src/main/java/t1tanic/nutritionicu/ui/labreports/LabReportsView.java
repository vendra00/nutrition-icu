package t1tanic.nutritionicu.ui.labreports;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import t1tanic.nutritionicu.dto.LabReportSummary;
import t1tanic.nutritionicu.service.ingestion.LabTestService;
import t1tanic.nutritionicu.service.patient.PatientOverviewService;
import t1tanic.nutritionicu.ui.MainLayout;
import t1tanic.nutritionicu.ui.common.UiFormat;
import t1tanic.nutritionicu.ui.patients.PatientOverviewDialog;

/** The ingested lab reports as a table (newest first), filterable by NHC, report date and department. */
@Route(value = "lab-reports", layout = MainLayout.class)
@PermitAll
public class LabReportsView extends VerticalLayout implements HasDynamicTitle {

    private static final int RECENT_LIMIT = 500;

    private final transient LabTestService labTestService;
    private final transient PatientOverviewService overviewService;
    private final Grid<LabReportSummary> grid = new Grid<>();

    private final TextField nhcFilter = new TextField();
    private final DatePicker dateFilter = new DatePicker();
    private final ComboBox<String> departmentFilter = new ComboBox<>();
    private transient GridListDataView<LabReportSummary> dataView;

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

        Column<LabReportSummary> nhcCol = grid.addComponentColumn(this::nhcLink)
                .setHeader(getTranslation("labreports.col.nhc")).setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(LabReportSummary::patientName)
                .setHeader(getTranslation("labreports.col.name")).setFlexGrow(1);
        grid.addComponentColumn(this::orderLink)
                .setHeader(getTranslation("labreports.col.order")).setAutoWidth(true).setFlexGrow(0);
        Column<LabReportSummary> deptCol = grid.addColumn(r -> nz(r.department()))
                .setHeader(getTranslation("labreports.col.department")).setAutoWidth(true).setFlexGrow(0);
        Column<LabReportSummary> dateCol = grid.addColumn(r -> UiFormat.date(r.reportDate()))
                .setHeader(getTranslation("labreports.col.date")).setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(LabReportSummary::sectionCount)
                .setHeader(getTranslation("labreports.col.sections")).setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(r -> UiFormat.instant(r.ingestedAt()))
                .setHeader(getTranslation("labreports.col.ingested")).setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(LabReportSummary::sourceFilename)
                .setHeader(getTranslation("labreports.col.file")).setAutoWidth(true).setFlexGrow(0);
        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        grid.setSizeFull();

        configureFilters(nhcCol, deptCol, dateCol);
        refresh();
        addAndExpand(grid);
    }

    /** Adds a filter row under the header with an NHC search, a report-date picker and a department selector. */
    private void configureFilters(Column<LabReportSummary> nhcCol, Column<LabReportSummary> deptCol,
                                  Column<LabReportSummary> dateCol) {
        nhcFilter.setPlaceholder(getTranslation("labreports.filter.nhc"));
        nhcFilter.setClearButtonVisible(true);
        nhcFilter.setValueChangeMode(ValueChangeMode.EAGER);
        nhcFilter.setWidthFull();
        nhcFilter.addValueChangeListener(e -> applyFilter());

        UiFormat.dayMonthYear(dateFilter);
        dateFilter.setPlaceholder(getTranslation("labreports.filter.date"));
        dateFilter.setClearButtonVisible(true);
        dateFilter.setWidthFull();
        dateFilter.addValueChangeListener(e -> applyFilter());

        departmentFilter.setPlaceholder(getTranslation("labreports.filter.department"));
        departmentFilter.setClearButtonVisible(true);
        departmentFilter.setWidthFull();
        departmentFilter.addValueChangeListener(e -> applyFilter());

        HeaderRow filterRow = grid.appendHeaderRow();
        filterRow.getCell(nhcCol).setComponent(nhcFilter);
        filterRow.getCell(dateCol).setComponent(dateFilter);
        filterRow.getCell(deptCol).setComponent(departmentFilter);
    }

    /** The NHC rendered as a link that opens the patient's overview, as on the Patients and Alerts tabs. */
    private Component nhcLink(LabReportSummary report) {
        Button link = new Button(report.patientMrn(),
                e -> new PatientOverviewDialog(report.patientId(), overviewService).open());
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        return link;
    }

    /** The order number (Petició) rendered as a link that opens the report's full detail view. */
    private Component orderLink(LabReportSummary report) {
        Button link = new Button(report.orderNumber(),
                e -> new LabReportDetailDialog(labTestService.reportDetail(report.id()), overviewService).open());
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        return link;
    }

    private void refresh() {
        List<LabReportSummary> reports = labTestService.recentReports(RECENT_LIMIT);
        dataView = grid.setItems(reports);
        departmentFilter.setItems(reports.stream()
                .map(LabReportSummary::department)
                .filter(d -> d != null && !d.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList());
        applyFilter();
    }

    private void applyFilter() {
        if (dataView == null) {
            return;
        }
        String nhc = nhcFilter.getValue() == null ? "" : nhcFilter.getValue().strip().toLowerCase();
        LocalDate date = dateFilter.getValue();
        String department = departmentFilter.getValue();
        dataView.setFilter(report ->
                (nhc.isEmpty() || contains(report.patientMrn(), nhc))
                        && (date == null || date.equals(report.reportDate()))
                        && (department == null || department.equals(report.department())));
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }

    private static String nz(String value) {
        return value == null || value.isBlank() ? UiFormat.EMPTY : value;
    }
}
