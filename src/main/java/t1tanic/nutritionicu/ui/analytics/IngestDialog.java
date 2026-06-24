package t1tanic.nutritionicu.ui.analytics;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.LinkedHashMap;
import java.util.Map;
import t1tanic.nutritionicu.dto.IngestionSummary;
import t1tanic.nutritionicu.dto.IngestionSummary.FileResult;
import t1tanic.nutritionicu.dto.IngestionSummary.Outcome;
import t1tanic.nutritionicu.service.ingestion.LabTestService;

/**
 * Lets a doctor upload one or more lab-report PDFs from their machine and ingest them. Files are saved into
 * the ingestion root and run through the normal pipeline (dedup, parsing, alerting); the per-file outcome is
 * listed so it's clear exactly which file was ingested, skipped, or failed (and why).
 */
public class IngestDialog extends Dialog {

    private final transient Map<String, byte[]> uploaded = new LinkedHashMap<>();
    private final Span summaryLabel = new Span();
    private final Grid<FileResult> results = new Grid<>();

    public IngestDialog(LabTestService labTestService, Runnable onIngested) {
        setHeaderTitle(getTranslation("ingest.title"));
        setWidth("560px");

        // The streams UploadHandler hands us each finished file's bytes in memory (no deprecated Receiver).
        UploadHandler handler = UploadHandler.inMemory(
                (metadata, data) -> uploaded.put(metadata.fileName(), data));
        Upload upload = new Upload(handler);
        upload.setAcceptedFileTypes("application/pdf", ".pdf");
        upload.setDropAllowed(true);
        upload.setWidthFull();
        upload.setI18n(uploadLabels());
        upload.addAllFinishedListener(event -> ingest(labTestService, onIngested, upload));

        summaryLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        summaryLabel.setVisible(false);

        results.addColumn(FileResult::filename).setHeader(getTranslation("ingest.col.file"))
                .setAutoWidth(true).setFlexGrow(0);
        results.addComponentColumn(this::outcomeBadge).setHeader(getTranslation("ingest.col.result")).setFlexGrow(1);
        results.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT,
                GridVariant.LUMO_COMPACT);
        results.setAllRowsVisible(true);
        results.setVisible(false);

        VerticalLayout content = new VerticalLayout(
                new Paragraph(getTranslation("ingest.intro")), upload, summaryLabel, results);
        content.setPadding(false);
        content.getStyle().set("gap", "var(--lumo-space-s)");
        add(content);
        getFooter().add(new Button(getTranslation("common.close"), e -> close()));
    }

    private void ingest(LabTestService service, Runnable onIngested, Upload upload) {
        if (uploaded.isEmpty()) {
            return;
        }
        IngestionSummary summary = service.ingestUploaded(uploaded);
        uploaded.clear();
        upload.clearFileList();

        summaryLabel.setText(getTranslation("ingest.result",
                summary.ingested(), summary.skipped(), summary.failed()));
        summaryLabel.setVisible(true);
        results.setItems(summary.results());
        results.setVisible(true);
        onIngested.run();
    }

    private Span outcomeBadge(FileResult result) {
        String label = getTranslation("ingest.outcome." + result.outcome().name());
        String text = result.outcome() == Outcome.FAILED && result.message() != null
                ? label + " — " + result.message()
                : label;
        Span badge = new Span(text);
        badge.getElement().getThemeList().add("badge " + theme(result.outcome()));
        badge.getStyle().set("white-space", "normal");
        return badge;
    }

    private static String theme(Outcome outcome) {
        return switch (outcome) {
            case INGESTED -> "success";
            case SKIPPED -> "contrast";
            case FAILED -> "error";
        };
    }

    private UploadI18N uploadLabels() {
        UploadI18N i18n = new UploadI18N();
        i18n.setAddFiles(new UploadI18N.AddFiles().setMany(getTranslation("ingest.upload")));
        i18n.setDropFiles(new UploadI18N.DropFiles().setMany(getTranslation("ingest.drop")));
        i18n.setError(new UploadI18N.Error().setIncorrectFileType(getTranslation("ingest.onlypdf")));
        return i18n;
    }
}
