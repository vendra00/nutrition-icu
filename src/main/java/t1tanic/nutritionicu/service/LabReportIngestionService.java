package t1tanic.nutritionicu.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.dto.IngestionSummary;
import t1tanic.nutritionicu.dto.ParsedReport;
import t1tanic.nutritionicu.model.LabReport;
import t1tanic.nutritionicu.model.LabResult;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.model.ReportSection;
import t1tanic.nutritionicu.repo.LabReportRepository;
import t1tanic.nutritionicu.repo.PatientRepository;

/**
 * Reads lab-report PDFs from a directory and stores them.
 * Ingestion is idempotent: a file already loaded (by filename or Petició) is skipped,
 * and each file is processed in its own transaction so one bad file can't roll back the rest.
 */
@Service
public class LabReportIngestionService {

    private static final Logger log = LoggerFactory.getLogger(LabReportIngestionService.class);

    private final PdfTextExtractor extractor;
    private final LabReportParser parser;
    private final AnalyteCatalog analyteCatalog;
    private final AlertService alertService;
    private final PatientRepository patientRepository;
    private final LabReportRepository reportRepository;
    private final LabReportIngestionService self;

    public LabReportIngestionService(PdfTextExtractor extractor,
                                     LabReportParser parser,
                                     AnalyteCatalog analyteCatalog,
                                     AlertService alertService,
                                     PatientRepository patientRepository,
                                     LabReportRepository reportRepository,
                                     @Lazy LabReportIngestionService self) {
        this.extractor = extractor;
        this.parser = parser;
        this.analyteCatalog = analyteCatalog;
        this.alertService = alertService;
        this.patientRepository = patientRepository;
        this.reportRepository = reportRepository;
        this.self = self; // call through the proxy so per-file @Transactional applies
    }

    /** Scans the given directory for *.pdf files and ingests each one. */
    public IngestionSummary ingestDirectory(Path dir) {
        if (!Files.isDirectory(dir)) {
            log.warn("Ingestion directory does not exist: {}", dir.toAbsolutePath());
            return new IngestionSummary(0, 0, 0, List.of("Directory not found: " + dir.toAbsolutePath()));
        }

        List<Path> pdfs = listPdfs(dir);
        log.info("Ingesting {} PDF(s) from {}", pdfs.size(), dir.toAbsolutePath());

        int ingested = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        for (Path pdf : pdfs) {
            try {
                if (self.ingestFile(pdf)) {
                    ingested++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                log.error("Failed to ingest {}", pdf.getFileName(), e);
                errors.add(pdf.getFileName() + ": " + e.getMessage());
            }
        }
        IngestionSummary summary = new IngestionSummary(ingested, skipped, errors.size(), errors);
        log.info("Ingestion done: {} ingested, {} skipped, {} failed", ingested, skipped, errors.size());
        return summary;
    }

    /**
     * Ingests a single PDF in its own transaction.
     *
     * @return true if stored, false if already present (skipped)
     */
    @Transactional
    public boolean ingestFile(Path pdf) {
        String filename = pdf.getFileName().toString();
        if (reportRepository.existsBySourceFilename(filename)) {
            return false;
        }

        String text = extractor.extract(pdf);
        ParsedReport parsed = parser.parse(filename, text);

        LabReport report = parsed.report();
        if (report.getOrderNumber() == null) {
            throw new IllegalStateException("No order number (Petició) found in " + filename);
        }
        if (reportRepository.existsByOrderNumber(report.getOrderNumber())) {
            return false;
        }

        Patient patient = resolvePatient(parsed.patient());
        report.setPatient(patient);
        enrichResults(report, patient);
        reportRepository.save(report); // cascades sections and results
        alertService.evaluate(report); // raise alerts for monitored patients with abnormal results
        log.debug("Ingested {} (order {}, {} sections)",
                filename, report.getOrderNumber(), report.getSections().size());
        return true;
    }

    /** Stamps each result with its patient (denormalized FK) and canonical analyte code. */
    private void enrichResults(LabReport report, Patient patient) {
        for (ReportSection section : report.getSections()) {
            for (LabResult result : section.getResults()) {
                result.setPatient(patient);
                result.setAnalyteCode(analyteCatalog.codeFor(result.getAnalyteName()));
            }
        }
    }

    /** Finds the patient by medical record number, refreshing demographics, or creates a new one. */
    private Patient resolvePatient(Patient parsed) {
        if (parsed.getMedicalRecordNumber() == null) {
            throw new IllegalStateException("No medical record number (NHC) found; cannot identify patient");
        }
        return patientRepository.findByMedicalRecordNumber(parsed.getMedicalRecordNumber())
                .map(existing -> updateDemographics(existing, parsed))
                .orElseGet(() -> patientRepository.save(parsed));
    }

    private Patient updateDemographics(Patient existing, Patient parsed) {
        if (parsed.getFullName() != null) {
            existing.setFullName(parsed.getFullName());
        }
        if (parsed.getBirthDate() != null) {
            existing.setBirthDate(parsed.getBirthDate());
        }
        if (parsed.getSex() != null && existing.getSex() == null) {
            existing.setSex(parsed.getSex());
        }
        if (parsed.getHealthCardId() != null) {
            existing.setHealthCardId(parsed.getHealthCardId());
        }
        if (parsed.getSocialSecurityNumber() != null) {
            existing.setSocialSecurityNumber(parsed.getSocialSecurityNumber());
        }
        return existing;
    }

    private List<Path> listPdfs(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list " + dir, e);
        }
    }
}
