package t1tanic.nutritionicu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application settings under the {@code app.*} prefix, bound at startup.
 *
 * <p>Note: {@code app.ingestion.on-startup} and {@code app.sandbox.seed} are read
 * directly by {@code @ConditionalOnProperty} on the startup runners, so they live in
 * {@code application.properties} but aren't bound here.
 */
@ConfigurationProperties("app")
public record AppProperties(@DefaultValue Ingestion ingestion, @DefaultValue Ocr ocr,
                            @DefaultValue Insights insights) {

    /** Where report PDFs are read from. */
    public record Ingestion(@DefaultValue("src/main/resources/data") String root) {
    }

    /** AI-insights knowledge base: a folder of reference study/guideline PDFs the model is grounded on. */
    public record Insights(@DefaultValue("src/main/resources/knowledge") String knowledgeRoot) {
    }

    /** OCR fallback settings for image-only/scanned PDFs. */
    public record Ocr(
            //tessdata directory (folder containing e.g. cat.traineddata); blank to use the default.
            @DefaultValue("") String tessdataPath,
            //Tesseract language(s); Catalan + Spanish for these reports.
            @DefaultValue("cat+spa") String language) {
    }
}
