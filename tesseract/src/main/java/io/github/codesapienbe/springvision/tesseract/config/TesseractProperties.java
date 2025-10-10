package io.github.codesapienbe.springvision.tesseract.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Vision Tesseract module.
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.vision.tesseract")
public class TesseractProperties {

    /**
     * Enable/disable Tesseract OCR backend.
     */
    private boolean enabled = false;

    /**
     * Path to tessdata directory containing language data files.
     */
    private String tessdataPath = System.getenv("TESSDATA_PREFIX");

    /**
     * Default language for OCR (ISO 639-3 code).
     */
    private String language = "eng";

    /**
     * Page segmentation mode (0-13).
     * 3 = Fully automatic page segmentation (default)
     */
    private int pageSegMode = 3;

    /**
     * OCR Engine Mode (0-3).
     * 1 = Neural nets LSTM engine only (default)
     */
    private int ocrEngineMode = 1;

    /**
     * Minimum confidence threshold for text detection (0-100).
     */
    private int minConfidence = 60;

    /**
     * Enable automatic language download.
     */
    private boolean enableAutoDownload = true;

    /**
     * Additional languages to load (comma-separated).
     */
    private String additionalLanguages = "";

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTessdataPath() {
        return tessdataPath;
    }

    public void setTessdataPath(String tessdataPath) {
        this.tessdataPath = tessdataPath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getPageSegMode() {
        return pageSegMode;
    }

    public void setPageSegMode(int pageSegMode) {
        this.pageSegMode = pageSegMode;
    }

    public int getOcrEngineMode() {
        return ocrEngineMode;
    }

    public void setOcrEngineMode(int ocrEngineMode) {
        this.ocrEngineMode = ocrEngineMode;
    }

    public int getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(int minConfidence) {
        this.minConfidence = minConfidence;
    }

    public boolean isEnableAutoDownload() {
        return enableAutoDownload;
    }

    public void setEnableAutoDownload(boolean enableAutoDownload) {
        this.enableAutoDownload = enableAutoDownload;
    }

    public String getAdditionalLanguages() {
        return additionalLanguages;
    }

    public void setAdditionalLanguages(String additionalLanguages) {
        this.additionalLanguages = additionalLanguages;
    }
}

