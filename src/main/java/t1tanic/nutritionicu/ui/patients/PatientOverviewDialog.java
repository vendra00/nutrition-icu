package t1tanic.nutritionicu.ui.patients;
import t1tanic.nutritionicu.ui.common.I18n;
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
        setHeaderTitle(I18n.t("overview.title") + " · " + (id.fullName() == null ? id.medicalRecordNumber() : id.fullName()));
        setWidth("640px");

        VerticalLayout content = new VerticalLayout(
                section(I18n.t("overview.section.identity"), identityTable(id)),
                section(I18n.t("overview.section.anthropometry"), anthropometryTable(overview.anthropometry())),
                section(I18n.t("overview.section.risk"), riskTable(overview.risk())));
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("gap", "var(--lumo-space-m)");
        add(content);

        getFooter().add(downloadButton(overview, overviewService), new Button(I18n.t("common.close"), e -> close()));
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
        Grid<MetricsTable.Row> grid = MetricsTable.create(I18n.t("overview.field"));
        grid.setItems(
                new MetricsTable.Row(I18n.t("overview.nhc"), nz(id.medicalRecordNumber())),
                new MetricsTable.Row(I18n.t("overview.name"), nz(id.fullName())),
                new MetricsTable.Row(I18n.t("overview.sex"),
                        id.sex() == null ? UiFormat.EMPTY : I18n.t("sex." + id.sex().name())),
                new MetricsTable.Row(I18n.t("overview.age"),
                        id.ageYears() == null ? UiFormat.EMPTY : I18n.t("overview.years", id.ageYears())),
                new MetricsTable.Row(I18n.t("overview.born"), UiFormat.date(id.birthDate())),
                new MetricsTable.Row(I18n.t("overview.diagnosis"),
                        id.admissionDiagnosis() == null ? UiFormat.EMPTY
                                : I18n.t("diagnosis." + id.admissionDiagnosis().name())),
                new MetricsTable.Row(I18n.t("overview.monitored"), I18n.t(id.monitored() ? "common.yes" : "common.no")),
                new MetricsTable.Row(I18n.t("overview.admitted"), UiFormat.date(id.admissionDate())),
                new MetricsTable.Row(I18n.t("overview.discharged"), UiFormat.date(id.dischargeDate())));
        return grid;
    }

    private static Grid<MetricsTable.Row> anthropometryTable(Anthropometry a) {
        Grid<MetricsTable.Row> grid = MetricsTable.create(I18n.t("overview.metric"));
        grid.setItems(
                new MetricsTable.Row(I18n.t("overview.height"), UiFormat.number(a.heightCm()) + " cm"),
                new MetricsTable.Row(I18n.t("overview.currentweight"), UiFormat.number(a.currentWeightKg()) + " kg"),
                new MetricsTable.Row(I18n.t("overview.usualweight"), UiFormat.number(a.usualWeightKg()) + " kg"),
                new MetricsTable.Row(I18n.t("overview.temperature"), temperature(a)),
                bmiRow(a),
                new MetricsTable.Row(I18n.t("overview.ibw"), UiFormat.number(a.idealBodyWeightKg()) + " kg"),
                new MetricsTable.Row(I18n.t("overview.abw"), UiFormat.number(a.adjustedBodyWeightKg()) + " kg"),
                new MetricsTable.Row(I18n.t("overview.weightloss"), UiFormat.number(a.weightLossPercent()) + " %"));
        return grid;
    }

    /** BMI row with a {@code *} marker + tooltip when the clinician flagged the BMI as misleading. */
    private static MetricsTable.Row bmiRow(Anthropometry a) {
        String value = UiFormat.number(a.bmi()) + (a.misleadingBmi() ? " *" : "");
        return new MetricsTable.Row(I18n.t("overview.bmi"), value, a.bmi(),
                a.misleadingBmi() ? I18n.t("bmi.misleading.tooltip") : null);
    }

    private static Grid<MetricsTable.Row> riskTable(Risk r) {
        Grid<MetricsTable.Row> grid = MetricsTable.create(I18n.t("overview.field"));
        if (r.present()) {
            grid.setItems(
                    new MetricsTable.Row(I18n.t("overview.score"), r.nutricScore() + " / " + r.nutricMax()),
                    new MetricsTable.Row(I18n.t("overview.risk"),
                            I18n.t(Boolean.TRUE.equals(r.highRisk()) ? "risk.high" : "risk.low")),
                    new MetricsTable.Row(I18n.t("overview.assessed"), UiFormat.date(r.assessedOn())));
        } else {
            grid.setItems(List.of(new MetricsTable.Row(I18n.t("overview.status"), I18n.t("overview.noassessment"))));
        }
        return grid;
    }

    private static Anchor downloadButton(PatientOverview overview, PatientOverviewService overviewService) {
        String fileName = "patient-" + nz(overview.identity().medicalRecordNumber()) + "-overview.pdf";
        DownloadHandler handler = DownloadHandler.fromInputStream(event -> {
            byte[] bytes = overviewService.toPdf(overview);
            return new DownloadResponse(new ByteArrayInputStream(bytes), fileName, "application/pdf", bytes.length);
        });

        Button button = new Button(I18n.t("overview.download"), VaadinIcon.DOWNLOAD.create());
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
