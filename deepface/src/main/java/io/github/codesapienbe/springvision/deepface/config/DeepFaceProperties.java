package io.github.codesapienbe.springvision.deepface.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for Spring Vision DeepFace module.
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.vision.deepface")
public class DeepFaceProperties {

    /**
     * Enable/disable DeepFace backend.
     */
    private boolean enabled = false;

    /**
     * Base URL of the DeepFace API server.
     */
    private String baseUrl = "http://localhost:5000";

    /**
     * Request timeout duration.
     */
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * Confidence threshold for face detection (0.0 - 1.0).
     */
    private double confidenceThreshold = 0.6;

    /**
     * Maximum number of faces to detect.
     */
    private int maxDetections = 10;

    /**
     * Face detection backend (opencv, ssd, dlib, mtcnn, retinaface, mediapipe).
     */
    private String detectorBackend = "opencv";

    /**
     * Face recognition model (VGG-Face, Facenet, Facenet512, OpenFace, DeepFace, DeepID, ArcFace, Dlib).
     */
    private String model = "VGG-Face";

    /**
     * Distance metric for face comparison (cosine, euclidean, euclidean_l2).
     */
    private String distanceMetric = "cosine";

    /**
     * Enable face alignment.
     */
    private boolean align = true;

    /**
     * Enable normalization.
     */
    private boolean normalization = true;

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public int getMaxDetections() {
        return maxDetections;
    }

    public void setMaxDetections(int maxDetections) {
        this.maxDetections = maxDetections;
    }

    public String getDetectorBackend() {
        return detectorBackend;
    }

    public void setDetectorBackend(String detectorBackend) {
        this.detectorBackend = detectorBackend;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDistanceMetric() {
        return distanceMetric;
    }

    public void setDistanceMetric(String distanceMetric) {
        this.distanceMetric = distanceMetric;
    }

    public boolean isAlign() {
        return align;
    }

    public void setAlign(boolean align) {
        this.align = align;
    }

    public boolean isNormalization() {
        return normalization;
    }

    public void setNormalization(boolean normalization) {
        this.normalization = normalization;
    }
}

