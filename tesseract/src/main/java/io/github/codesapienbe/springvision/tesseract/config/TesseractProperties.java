package io.github.codesapienbe.springvision.tesseract.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Spring Vision Tesseract module.
 *
 * @param enabled             whether Tesseract backend is enabled
 * @param tessdataPath        path to the Tesseract training data
 * @param language            language code for OCR (e.g., 'eng' for English)
 * @param pageSegMode         page segmentation mode
 * @param ocrEngineMode       OCR engine mode
 * @param minConfidence       minimum confidence threshold for OCR results
 * @param enableAutoDownload  whether to automatically download language data
 * @param additionalLanguages comma-separated list of additional languages
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "spring.vision.tesseract")
public record TesseractProperties(
    boolean enabled,
    String tessdataPath,
    String language,
    int pageSegMode,
    int ocrEngineMode,
    int minConfidence,
    boolean enableAutoDownload,
    String additionalLanguages
) {
    /**
     * Default constructor with default values.
     */
    public TesseractProperties() {
        this(
            false,
            System.getenv("TESSDATA_PREFIX"),
            "eng",
            3,
            1,
            60,
            true,
            ""
        );
    }
}
