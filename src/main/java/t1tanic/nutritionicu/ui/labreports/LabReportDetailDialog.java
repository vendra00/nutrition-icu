package t1tanic.nutritionicu.ui.labreports;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import t1tanic.nutritionicu.dto.LabReportDetail;
import t1tanic.nutritionicu.service.patient.PatientOverviewService;
import t1tanic.nutritionicu.ui.common.UiFormat;
import t1tanic.nutritionicu.ui.patients.PatientOverviewDialog;

/**
 * Read-only full view of a single lab report, opened from the Order (Petició) link in the Lab reports grid.
 * Shows the report's header fields as a Field / Value table (the patient NHC links to the same overview used
 * on the Patients tab) followed by each section as a heading plus an Analyte / Value / Unit / Flag / Reference
 * grid — mirroring how the source PDF is laid out.
 */
public class LabReportDetailDialog extends Dialog {

    private record Field(String field, Component value) {
    }

    private final transient PatientOverviewService overviewService;

    public LabReportDetailDialog(LabReportDetail report, PatientOverviewService overviewService) {
        this.overviewService = overviewService;
        setHeaderTitle(getTranslation("labreports.detail.title", report.orderNumber()));
        setWidth("680px");
        setMaxHeight("85vh");

        VerticalLayout content = new VerticalLayout(headerTable(report));
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("gap", "var(--lumo-space-m)");

        for (LabReportDetail.Section section : report.sections()) {
            content.add(sectionHeading(section));
            if (section.validatedBy() != null && !section.validatedBy().isBlank()) {
                Span validated = new Span(getTranslation("labreports.detail.validatedby", section.validatedBy()));
                validated.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
                content.add(validated);
            }
            content.add(rowsGrid(section.rows()));
        }

        add(content);
        getFooter().add(new Button(getTranslation("common.close"), e -> close()));
    }

    private Grid<Field> headerTable(LabReportDetail report) {
        List<Field> fields = new ArrayList<>();
        fields.add(new Field(getTranslation("labreports.col.nhc"), patientValue(report)));
        fields.add(new Field(getTranslation("labreports.col.name"), text(report.patientName())));
        fields.add(new Field(getTranslation("labreports.col.order"), text(report.orderNumber())));
        addIfPresent(fields, "labreports.field.reference", report.reference());
        addIfPresent(fields, "labreports.col.department", report.department());
        addIfPresent(fields, "labreports.field.center", report.center());
        addIfPresent(fields, "labreports.field.physician", report.requestingPhysician());
        if (report.ageYearsAtReport() != null) {
            fields.add(new Field(getTranslation("labreports.field.age"),
                    text(getTranslation("common.years", String.valueOf(report.ageYearsAtReport())))));
        }
        fields.add(new Field(getTranslation("labreports.col.date"), text(UiFormat.date(report.reportDate()))));
        fields.add(new Field(getTranslation("labreports.field.reception"), text(UiFormat.dateTime(report.receptionAt()))));
        fields.add(new Field(getTranslation("labreports.field.finalization"), text(UiFormat.dateTime(report.finalizationAt()))));
        fields.add(new Field(getTranslation("labreports.col.file"), text(report.sourceFilename())));

        Grid<Field> grid = new Grid<>();
        grid.addColumn(Field::field).setHeader(getTranslation("overview.field")).setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(Field::value).setHeader(getTranslation("common.value")).setFlexGrow(1);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT, GridVariant.LUMO_COMPACT);
        grid.setAllRowsVisible(true);
        grid.setItems(fields);
        return grid;
    }

    private Span sectionHeading(LabReportDetail.Section section) {
        String category = localizedHeading(section.category());
        String name = localizedHeading(section.name());
        String title = name == null || name.isBlank() ? category : category + " › " + name;
        Span heading = new Span(title);
        heading.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.Margin.Top.SMALL);
        return heading;
    }

    /**
     * Localized section heading: the raw (Catalan) heading is normalized to a {@code section.<SLUG>} key
     * (accents stripped, non-alphanumerics collapsed to {@code _}); a missing key falls back to the raw text,
     * so an as-yet-untranslated heading still shows rather than a key.
     */
    private String localizedHeading(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String key = "section." + slug(raw);
        String translated = getTranslation(key);
        return translated.equals(key) ? raw : translated;
    }

    private static String slug(String raw) {
        String noAccents = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return noAccents.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private Grid<LabReportDetail.Row> rowsGrid(List<LabReportDetail.Row> rows) {
        Grid<LabReportDetail.Row> grid = new Grid<>();
        grid.addColumn(this::localizedAnalyte)
                .setHeader(getTranslation("analytics.col.analyte")).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(r -> nz(r.value())).setHeader(getTranslation("analytics.col.value")).setAutoWidth(true);
        grid.addColumn(r -> nz(r.unit())).setHeader(getTranslation("analytics.col.unit")).setAutoWidth(true);
        grid.addComponentColumn(this::flagBadge).setHeader(getTranslation("analytics.col.flag")).setAutoWidth(true);
        grid.addColumn(r -> nz(r.reference())).setHeader(getTranslation("analytics.col.reference")).setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setAllRowsVisible(true);
        grid.setItems(rows);
        return grid;
    }

    /**
     * Localized analyte name: a canonical code resolves to its {@code analyte.code.<CODE>} translation (so it
     * follows the app language); an unmapped analyte falls back to the catalog English / raw printed label.
     */
    private String localizedAnalyte(LabReportDetail.Row row) {
        return row.code() == null ? row.analyte() : getTranslation("analyte.code." + row.code());
    }

    /** The abnormality flag as a coloured badge; nothing for a normal/absent flag to keep the grid calm. */
    private Component flagBadge(LabReportDetail.Row row) {
        String flag = row.flag();
        if (flag == null || "NORMAL".equals(flag)) {
            return new Span();
        }
        Span badge = new Span(getTranslation("resultFlag." + flag));
        String theme = switch (flag) {
            case "VERY_HIGH", "VERY_LOW" -> "badge error";
            case "HIGH", "LOW" -> "badge warning";
            default -> "badge";
        };
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    /** The NHC as a link that opens the patient overview, or plain text when no patient id is available. */
    private Component patientValue(LabReportDetail report) {
        if (report.patientId() == null) {
            return text(report.patientMrn());
        }
        Button link = new Button(report.patientMrn(),
                e -> new PatientOverviewDialog(report.patientId(), overviewService).open());
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        return link;
    }

    private void addIfPresent(List<Field> fields, String labelKey, String value) {
        if (value != null && !value.isBlank()) {
            fields.add(new Field(getTranslation(labelKey), text(value)));
        }
    }

    private static String nz(String value) {
        return value == null || value.isBlank() ? UiFormat.EMPTY : value;
    }

    private static Span text(String value) {
        Span span = new Span(value == null || value.isBlank() ? UiFormat.EMPTY : value);
        span.getStyle().set("white-space", "normal");
        return span;
    }
}
