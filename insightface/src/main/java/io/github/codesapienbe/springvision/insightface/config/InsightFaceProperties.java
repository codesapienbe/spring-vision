package io.github.codesapienbe.springvision.insightface.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Vision InsightFace module.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.vision.insightface")
public class InsightFaceProperties {

    /**
     * Enable/disable InsightFace backend.
     */
    private boolean enabled = false;

    /**
     * API URL for InsightFace service.
     */
    private String apiUrl = "http://localhost:8000";

    /**
     * API key for authentication.
     */
    private String apiKey = "";

    /**
     * Model name (buffalo_l, buffalo_m, buffalo_s).
     */
    private String modelName = "buffalo_l";

    /**
     * Confidence threshold for face detection (0.0 - 1.0).
     */
    private double confidenceThreshold = 0.5;

    /**
     * Verification threshold for face matching (0.0 - 1.0).
     */
    private double verificationThreshold = 0.6;

    /**
     * Maximum number of faces to detect.
     */
    private int maxDetections = 10;

    /**
     * Enable age and gender analysis.
     */
    private boolean enableAgeGender = true;

    /**
     * Enable emotion detection.
     */
    private boolean enableEmotion = true;

    /**
     * Enable landmark detection.
     */
    private boolean enableLandmarks = true;

    /**
     * Request timeout in seconds.
     */
    private int timeoutSeconds = 30;

    /**
     * Maximum retry attempts.
     */
    private int maxRetries = 3;

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public double getVerificationThreshold() {
        return verificationThreshold;
    }

    public void setVerificationThreshold(double verificationThreshold) {
        this.verificationThreshold = verificationThreshold;
    }

    public int getMaxDetections() {
        return maxDetections;
    }

    public void setMaxDetections(int maxDetections) {
        this.maxDetections = maxDetections;
    }

    public boolean isEnableAgeGender() {
        return enableAgeGender;
    }

    public void setEnableAgeGender(boolean enableAgeGender) {
        this.enableAgeGender = enableAgeGender;
    }

    public boolean isEnableEmotion() {
        return enableEmotion;
    }

    public void setEnableEmotion(boolean enableEmotion) {
        this.enableEmotion = enableEmotion;
    }

    public boolean isEnableLandmarks() {
        return enableLandmarks;
    }

    public void setEnableLandmarks(boolean enableLandmarks) {
        this.enableLandmarks = enableLandmarks;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}

