package t1tanic.nutritionicu.ui.patients;
import t1tanic.nutritionicu.ui.common.MetricsTable;
import t1tanic.nutritionicu.ui.common.UiFormat;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import java.io.ByteArrayInputStream;
import java.util.List;
import t1tanic.nutritionicu.dto.PatientOverview;
import t1tanic.nutritionicu.dto.PatientOverview.Anthropometry;
import t1tanic.nutritionicu.dto.PatientOverview.Identity;
import t1tanic.nutritionicu.dto.PatientOverview.Risk;
import t1tanic.nutritionicu.service.patient.PatientOverviewService;

/**
 * Read-only overview of a patient's key nutrition values and basic stats, opened from the NHC link in
 * the Patients tab. Offers a one-page PDF download of the same snapshot.
 */
public class PatientOverviewDialog extends Dialog {

    public PatientOverviewDialog(Long patientId, PatientOverviewService overviewService) {
        PatientOverview overview = overviewService.build(patientId);
        Identity id = overview.identity();
        setHeaderTitle("Overview · " + (id.fullName() == null ? id.medicalRecordNumber() : id.fullName()));
        setWidth("640px");

        VerticalLayout content = new VerticalLayout(
                section("Identity", identityTable(id)),
                section("Anthropometry & nutrition", anthropometryTable(overview.anthropometry())),
                section("Nutritional risk (NUTRIC)", riskTable(overview.risk())));
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("gap", "var(--lumo-space-m)");
        add(content);

        getFooter().add(downloadButton(overview, overviewService), new Button("Close", e -> close()));
    }

    private static VerticalLayout section(String title, Grid<MetricsTable.Row> table) {
        H4 heading = new H4(title);
        heading.getStyle().set("margin", "0");
        table.setWidthFull();
        VerticalLayout box = new VerticalLayout(heading, table);
        box.setPadding(false);
        box.setSpacing(false);
        box.getStyle().set("gap", "var(--lumo-space-xs)");
        return box;
    }

    private static Grid<MetricsTable.Row> identityTable(Identity id) {
        Grid<MetricsTable.Row> grid = MetricsTable.create("Field");
        grid.setItems(
                new MetricsTable.Row("NHC", nz(id.medicalRecordNumber())),
                new MetricsTable.Row("Name", nz(id.fullName())),
                new MetricsTable.Row("Sex", id.sex() == null ? UiFormat.EMPTY : id.sex().name()),
                new MetricsTable.Row("Age", id.ageYears() == null ? UiFormat.EMPTY : id.ageYears() + " yrs"),
                new MetricsTable.Row("Born", UiFormat.date(id.birthDate())),
                new MetricsTable.Row("Monitored", id.monitored() ? "Yes" : "No"),
                new MetricsTable.Row("Admitted", UiFormat.date(id.admissionDate())),
                new MetricsTable.Row("Discharged", UiFormat.date(id.dischargeDate())));
        return grid;
    }

    private static Grid<MetricsTable.Row> anthropometryTable(Anthropometry a) {
        Grid<MetricsTable.Row> grid = MetricsTable.create("Metric");
        grid.setItems(
                new MetricsTable.Row("Height", UiFormat.number(a.heightCm()) + " cm"),
                new MetricsTable.Row("Current weight", UiFormat.number(a.currentWeightKg()) + " kg"),
                new MetricsTable.Row("Usual weight", UiFormat.number(a.usualWeightKg()) + " kg"),
                new MetricsTable.Row("Temperature (latest)", temperature(a)),
                new MetricsTable.Row("BMI", UiFormat.number(a.bmi()), a.bmi()),
                new MetricsTable.Row("Ideal body weight", UiFormat.number(a.idealBodyWeightKg()) + " kg"),
                new MetricsTable.Row("Adjusted body weight", UiFormat.number(a.adjustedBodyWeightKg()) + " kg"),
                new MetricsTable.Row("Recent weight loss", UiFormat.number(a.weightLossPercent()) + " %"));
        return grid;
    }

    private static Grid<MetricsTable.Row> riskTable(Risk r) {
        Grid<MetricsTable.Row> grid = MetricsTable.create("Field");
        if (r.present()) {
            grid.setItems(
                    new MetricsTable.Row("NUTRIC score", r.nutricScore() + " / " + r.nutricMax()),
                    new MetricsTable.Row("Risk", Boolean.TRUE.equals(r.highRisk()) ? "High risk" : "Low risk"),
                    new MetricsTable.Row("Assessed", UiFormat.date(r.assessedOn())));
        } else {
            grid.setItems(List.of(new MetricsTable.Row("Status", "No assessment recorded")));
        }
        return grid;
    }

    private static Anchor downloadButton(PatientOverview overview, PatientOverviewService overviewService) {
        String fileName = "patient-" + nz(overview.identity().medicalRecordNumber()) + "-overview.pdf";
        DownloadHandler handler = DownloadHandler.fromInputStream(event -> {
            byte[] bytes = overviewService.toPdf(overview);
            return new DownloadResponse(new ByteArrayInputStream(bytes), fileName, "application/pdf", bytes.length);
        });

        Button button = new Button("Download PDF", VaadinIcon.DOWNLOAD.create());
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Anchor anchor = new Anchor(handler, "");
        anchor.add(button);
        return anchor;
    }

    private static String temperature(Anthropometry a) {
        if (a.latestTemperatureC() == null) {
            return UiFormat.EMPTY;
        }
        String value = UiFormat.number(a.latestTemperatureC()) + " °C";
        return a.latestTemperatureDate() == null ? value : value + " · " + UiFormat.date(a.latestTemperatureDate());
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? UiFormat.EMPTY : s;
    }
}
