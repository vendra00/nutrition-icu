package t1tanic.nutritionicu.service.ingestion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t1tanic.nutritionicu.config.AppProperties;
import t1tanic.nutritionicu.dto.IngestionSummary;
import t1tanic.nutritionicu.dto.LabReportDetail;
import t1tanic.nutritionicu.dto.LabReportSummary;
import t1tanic.nutritionicu.exception.ResourceNotFoundException;
import t1tanic.nutritionicu.exception.ValidationException;
import t1tanic.nutritionicu.model.LabReport;
import t1tanic.nutritionicu.model.Patient;
import t1tanic.nutritionicu.repo.LabReportRepository;
import t1tanic.nutritionicu.service.lab.AnalyteCatalog;

/**
 * Resolves the front-end's requested folder against a configured root, guards
 * against escaping that root, and delegates the actual work to the ingestion engine.
 */
@Slf4j
@Service
public class LabTestServiceImpl implements LabTestService {

    private final LabReportIngestionService ingestionService;
    private final LabReportRepository reportRepository;
    private final AnalyteCatalog analyteCatalog;
    private final Path root;

    public LabTestServiceImpl(LabReportIngestionService ingestionService,
                              LabReportRepository reportRepository, AnalyteCatalog analyteCatalog,
                              AppProperties properties) {
        this.ingestionService = ingestionService;
        this.reportRepository = reportRepository;
        this.analyteCatalog = analyteCatalog;
        this.root = Path.of(properties.ingestion().root()).toAbsolutePath().normalize();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabReportSummary> recentReports(int limit) {
        return reportRepository.findRecentSummaries(PageRequest.of(0, Math.max(limit, 1)));
    }

    @Override
    @Transactional(readOnly = true)
    public LabReportDetail reportDetail(Long reportId) {
        LabReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Lab report " + reportId + " not found"));
        Patient patient = report.getPatient();
        // Within the transaction: walking sections -> results triggers the lazy loads, then we map to a DTO.
        List<LabReportDetail.Section> sections = report.getSections().stream()
                .map(section -> new LabReportDetail.Section(
                        section.getCategory(),
                        section.getName(),
                        section.getValidatedBy(),
                        section.getResults().stream()
                                .map(r -> new LabReportDetail.Row(
                                        analyteCatalog.codeFor(r.getAnalyteName()),
                                        analyteCatalog.displayName(r.getAnalyteName()),
                                        r.getValueRaw(),
                                        r.getUnitRaw(),
                                        r.getFlag() == null ? null : r.getFlag().name(),
                                        r.getRefRaw()))
                                .toList()))
                .toList();
        return new LabReportDetail(
                patient.getId(), patient.getMedicalRecordNumber(), patient.getFullName(),
                report.getOrderNumber(), report.getReference(), report.getDepartment(), report.getCenter(),
                report.getRequestingPhysician(), report.getReportDate(),
                report.getReceptionAt(), report.getFinalizationAt(),
                report.getAgeYearsAtReport(), report.getSourceFilename(), sections);
    }

    @Override
    public IngestionSummary ingest(String relativePath) {
        Path target = resolveWithinRoot(relativePath);
        log.info("Ingesting from {}", target);
        return ingestionService.ingestDirectory(target);
    }

    @Override
    public IngestionSummary ingestUploaded(Map<String, byte[]> filesByName) {
        List<Path> saved = new ArrayList<>();
        try {
            Files.createDirectories(root);
            for (Map.Entry<String, byte[]> entry : filesByName.entrySet()) {
                Path target = root.resolve(safeName(entry.getKey())).normalize();
                if (!target.startsWith(root)) {
                    throw new ValidationException("Invalid file name: " + entry.getKey());
                }
                if (!Files.exists(target)) { // keep an existing file (and its already-ingested data) intact
                    Files.write(target, entry.getValue());
                }
                saved.add(target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save uploaded PDF(s)", e);
        }
        log.info("Ingesting {} uploaded PDF(s) into {}", saved.size(), root);
        return ingestionService.ingestFiles(saved);
    }

    /** Strips any path components from an uploaded file name, keeping just the base file name. */
    private static String safeName(String filename) {
        String base = Path.of(filename.replace('\\', '/')).getFileName().toString();
        return base.isBlank() ? "upload.pdf" : base;
    }

    /** Resolves a relative subfolder under the root, rejecting anything that escapes it. */
    private Path resolveWithinRoot(String relativePath) {
        String relative = relativePath == null ? "" : relativePath.strip();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new ValidationException(
                    "Path '" + relativePath + "' resolves outside the ingestion root");
        }
        return target;
    }
}
