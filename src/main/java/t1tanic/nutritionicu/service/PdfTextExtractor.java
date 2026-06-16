package t1tanic.nutritionicu.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Extracts plain text from a lab-report PDF.
 *
 * <p>Primary path is PDFBox's text layer, which these reports are generated with —
 * fast and exact, no digit-mangling. If a PDF turns out to be image-only (scanned),
 * the extracted text is effectively empty and we fall back to Tesseract OCR.
 */
@Component
public class PdfTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);

    /** Below this many non-whitespace chars we treat the text layer as absent. */
    private static final int MIN_TEXT_LAYER_CHARS = 20;

    /** Render DPI for the OCR fallback; 300 is the usual sweet spot for documents. */
    private static final int OCR_DPI = 300;

    /** tessdata directory (folder containing e.g. cat.traineddata). Optional. */
    @Value("${app.ocr.tessdata-path:}")
    private String tessdataPath;

    /** Tesseract language(s) for the fallback; Catalan + Spanish for these reports. */
    @Value("${app.ocr.language:cat+spa}")
    private String ocrLanguage;

    /**
     * Returns the full text of the PDF, using OCR only if the text layer is missing.
     *
     * @throws UncheckedIOException if the file cannot be read as a PDF
     */
    public String extract(Path pdf) {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            String text = extractTextLayer(document);
            if (countNonWhitespace(text) >= MIN_TEXT_LAYER_CHARS) {
                return text;
            }
            log.info("No usable text layer in {} ({} chars) - falling back to OCR",
                    pdf.getFileName(), countNonWhitespace(text));
            return ocr(document);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read PDF: " + pdf, e);
        }
    }

    private String extractTextLayer(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        // Keep the visual top-to-bottom, left-to-right reading order.
        stripper.setSortByPosition(true);
        return stripper.getText(document);
    }

    private String ocr(PDDocument document) throws IOException {
        Tesseract tesseract = new Tesseract();
        if (tessdataPath != null && !tessdataPath.isBlank()) {
            tesseract.setDatapath(tessdataPath);
        }
        tesseract.setLanguage(ocrLanguage);

        PDFRenderer renderer = new PDFRenderer(document);
        List<String> pages = new ArrayList<>();
        for (int page = 0; page < document.getNumberOfPages(); page++) {
            BufferedImage image = renderer.renderImageWithDPI(page, OCR_DPI, ImageType.GRAY);
            try {
                pages.add(tesseract.doOCR(image));
            } catch (TesseractException e) {
                throw new IllegalStateException(
                        "OCR failed on page " + (page + 1) + ". Is Tesseract/tessdata available? "
                                + "Set app.ocr.tessdata-path if needed.", e);
            }
        }
        return String.join("\n", pages);
    }

    private static int countNonWhitespace(String text) {
        if (text == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }
}
