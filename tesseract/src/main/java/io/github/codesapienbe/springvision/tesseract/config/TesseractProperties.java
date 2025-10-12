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

    /**
     * Default constructor for TesseractProperties.
     */
    public TesseractProperties() {
        // Default constructor
    }

    // Getters and Setters

    /**
     * @return whether the Tesseract backend is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled whether to enable the Tesseract backend.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the path to the tessdata directory.
     */
    public String getTessdataPath() {
        return tessdataPath;
    }

    /**
     * @param tessdataPath the path to the tessdata directory.
     */
    public void setTessdataPath(String tessdataPath) {
        this.tessdataPath = tessdataPath;
    }

    /**
     * @return the default language for OCR.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language the default language for OCR.
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @return the page segmentation mode.
     */
    public int getPageSegMode() {
        return pageSegMode;
    }

    /**
     * @param pageSegMode the page segmentation mode.
     */
    public void setPageSegMode(int pageSegMode) {
        this.pageSegMode = pageSegMode;
    }

    /**
     * @return the OCR engine mode.
     */
    public int getOcrEngineMode() {
        return ocrEngineMode;
    }

    /**
     * @param ocrEngineMode the OCR engine mode.
     */
    public void setOcrEngineMode(int ocrEngineMode) {
        this.ocrEngineMode = ocrEngineMode;
    }

    /**
     * @return the minimum confidence threshold.
     */
    public int getMinConfidence() {
        return minConfidence;
    }

    /**
     * @param minConfidence the minimum confidence threshold.
     */
    public void setMinConfidence(int minConfidence) {
        this.minConfidence = minConfidence;
    }

    /**
     * @return whether automatic language download is enabled.
     */
    public boolean isEnableAutoDownload() {
        return enableAutoDownload;
    }

    /**
     * @param enableAutoDownload whether to enable automatic language download.
     */
    public void setEnableAutoDownload(boolean enableAutoDownload) {
        this.enableAutoDownload = enableAutoDownload;
    }

    /**
     * @return the additional languages to load.
     */
    public String getAdditionalLanguages() {
        return additionalLanguages;
    }

    /**
     * @param additionalLanguages the additional languages to load.
     */
    public void setAdditionalLanguages(String additionalLanguages) {
        this.additionalLanguages = additionalLanguages;
    }
}
