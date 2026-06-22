package t1tanic.nutritionicu.service.insight;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import t1tanic.nutritionicu.config.AppProperties;
import t1tanic.nutritionicu.dto.KnowledgeRef;
import t1tanic.nutritionicu.service.ingestion.PdfTextExtractor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Reference library for AI insights: extracts the text of study/guideline PDFs dropped into the
 * configured knowledge folder and exposes it as one block to ground the model. Also derives a readable
 * title per PDF (metadata, falling back to a heuristic then the file name) and serves the files so the
 * references can be cited and opened. Loaded once and cached; call {@link #reload()} after changes.
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    /** Cap on the combined knowledge text sent to the model, to bound token cost. */
    private static final int MAX_TOTAL_CHARS = 240_000;

    /** Optional sidecar mapping a PDF file name to its curated (Vancouver) citation. */
    private static final String CITATIONS_FILE = "references.json";

    private final Path root;
    private final PdfTextExtractor extractor;
    private final ObjectMapper objectMapper;

    private volatile List<Document> documents;

    public KnowledgeBaseService(AppProperties properties, PdfTextExtractor extractor, ObjectMapper objectMapper) {
        this.root = Path.of(properties.insights().knowledgeRoot());
        this.extractor = extractor;
        this.objectMapper = objectMapper;
    }

    /** A loaded reference document: file name, derived title and extracted text. */
    public record Document(String name, String title, String text) {
    }

    public boolean hasDocuments() {
        return !documents().isEmpty();
    }

    /** The loaded references (file name + title), for citation. */
    public List<KnowledgeRef> references() {
        return documents().stream().map(d -> new KnowledgeRef(d.name(), d.title())).toList();
    }

    /** References serialised one per line as {@code name\ttitle}, for storing on a saved insight. */
    public String referencesForStorage() {
        return documents().stream()
                .map(d -> d.name() + "\t" + d.title())
                .collect(Collectors.joining("\n"));
    }

    /**
     * The references concatenated with per-document headers, capped at {@link #MAX_TOTAL_CHARS}.
     * Empty string when the folder has no usable PDFs.
     */
    public String combinedText() {
        List<Document> docs = documents();
        if (docs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (Document doc : docs) {
            if (sb.length() >= MAX_TOTAL_CHARS) {
                sb.append("\n[Additional references omitted to stay within the size limit.]");
                break;
            }
            sb.append("\n===== Reference ").append(index++).append(": ").append(doc.title())
                    .append(" (").append(doc.name()).append(") =====\n");
            int remaining = MAX_TOTAL_CHARS - sb.length();
            sb.append(doc.text().length() > remaining ? doc.text().substring(0, remaining) : doc.text());
            sb.append('\n');
        }
        return sb.toString().strip();
    }

    /** Resolves a reference file name to a path inside the knowledge folder (rejects traversal). */
    public Optional<Path> resolve(String name) {
        if (name == null || name.isBlank() || name.contains("/") || name.contains("\\") || name.contains("..")) {
            return Optional.empty();
        }
        Path candidate = root.resolve(name).normalize();
        if (!candidate.startsWith(root.normalize()) || !Files.isRegularFile(candidate)) {
            return Optional.empty();
        }
        return Optional.of(candidate);
    }

    public boolean exists(String name) {
        return resolve(name).isPresent();
    }

    /** Reads a reference PDF's bytes for serving it to the browser. */
    public byte[] read(String name) {
        Path path = resolve(name).orElseThrow(() -> new IllegalArgumentException("Unknown reference: " + name));
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read reference: " + name, e);
        }
    }

    /** Drops the cache so the next access re-reads the folder. */
    public void reload() {
        documents = null;
    }

    private List<Document> documents() {
        List<Document> cached = documents;
        if (cached == null) {
            synchronized (this) {
                if (documents == null) {
                    documents = load();
                }
                cached = documents;
            }
        }
        return cached;
    }

    private List<Document> load() {
        if (!Files.isDirectory(root)) {
            log.info("AI knowledge base: folder {} not present - no references loaded", root);
            return List.of();
        }
        Map<String, String> citations = loadCitations();
        List<Document> loaded = new ArrayList<>();
        try (var stream = Files.list(root)) {
            List<Path> pdfs = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .toList();
            for (Path pdf : pdfs) {
                try {
                    String text = extractor.extract(pdf).strip();
                    if (!text.isBlank()) {
                        String name = pdf.getFileName().toString();
                        loaded.add(new Document(name, titleFor(pdf, text, citations.get(name)), text));
                    } else {
                        log.warn("AI knowledge base: {} yielded no text - skipped", pdf.getFileName());
                    }
                } catch (UncheckedIOException | IllegalStateException e) {
                    log.warn("AI knowledge base: failed to read {} - skipped ({})", pdf.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list knowledge folder: " + root, e);
        }
        log.info("AI knowledge base: loaded {} reference document(s) from {} ({} curated citation(s))",
                loaded.size(), root, citations.size());
        return loaded;
    }

    /** Reads the optional {@code references.json} (file name -> citation); empty when absent or invalid. */
    private Map<String, String> loadCitations() {
        Path file = root.resolve(CITATIONS_FILE);
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }
        try (InputStream in = Files.newInputStream(file)) {
            Map<String, String> map = objectMapper.readValue(in, new TypeReference<Map<String, String>>() {});
            return map == null ? Map.of() : map;
        } catch (IOException | RuntimeException e) {
            log.warn("AI knowledge base: could not read {} - ignoring ({})", CITATIONS_FILE, e.getMessage());
            return Map.of();
        }
    }

    /** Curated citation if the sidecar provides one, else the title derived from the PDF. */
    private static String titleFor(Path pdf, String text, String citation) {
        if (citation != null && !citation.isBlank()) {
            return citation.strip();
        }
        return titleOf(pdf, text);
    }

    /** PDF metadata title, else the first plausible line of text, else the file name without extension. */
    private static String titleOf(Path pdf, String text) {
        String metaTitle = metadataTitle(pdf);
        if (looksLikeTitle(metaTitle)) {
            return metaTitle.strip();
        }
        String firstLine = firstMeaningfulLine(text);
        if (looksLikeTitle(firstLine)) {
            return firstLine;
        }
        String fileName = pdf.getFileName().toString();
        return fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? fileName.substring(0, fileName.length() - 4)
                : fileName;
    }

    private static String metadataTitle(Path pdf) {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            return document.getDocumentInformation() == null ? null : document.getDocumentInformation().getTitle();
        } catch (IOException e) {
            return null;
        }
    }

    private static String firstMeaningfulLine(String text) {
        for (String line : text.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.length() >= 12) {
                return trimmed;
            }
        }
        return null;
    }

    private static boolean looksLikeTitle(String candidate) {
        if (candidate == null) {
            return false;
        }
        String t = candidate.strip();
        String lower = t.toLowerCase(Locale.ROOT);
        return t.length() >= 8 && t.length() <= 200
                && t.contains(" ")
                && t.chars().anyMatch(Character::isLowerCase)
                && !lower.startsWith("microsoft word")
                && !lower.startsWith("untitled");
    }
}
