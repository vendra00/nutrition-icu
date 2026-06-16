package t1tanic.nutritionicu.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import t1tanic.nutritionicu.dto.IngestionSummary;
import t1tanic.nutritionicu.service.LabTestService;

/** HTTP entry point for ingesting lab-report PDFs from a chosen folder. */
@RestController
@RequestMapping("/api/lab-tests")
public class LabTestController {

    private final LabTestService labTestService;

    public LabTestController(LabTestService labTestService) {
        this.labTestService = labTestService;
    }

    /**
     * Ingests all PDFs in {@code path} (a subfolder of the configured root).
     * Omit {@code path} to ingest the root folder itself.
     *
     * <p>Example: {@code POST /api/lab-tests/ingest?path=06-08}
     */
    @PostMapping("/ingest")
    public IngestionSummary ingest(@RequestParam(required = false) String path) {
        return labTestService.ingest(path);
    }
}
