package io.github.codesapienbe.springvision.yolo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Vision YOLO module.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.vision.yolo")
public class YoloProperties {

    /**
     * Enable/disable YOLO backend.
     */
    private boolean enabled = false;

    /**
     * Model path for YOLO models.
     */
    private String modelPath = "~/.spring-vision/models/yolo";

    /**
     * Model name (yolov8n.onnx, yolov8s.onnx, yolov8m.onnx).
     */
    private String modelName = "yolov8n.onnx";

    /**
     * Confidence threshold for detection (0.0 - 1.0).
     */
    private double confidenceThreshold = 0.25;

    /**
     * Non-maximum suppression threshold (0.0 - 1.0).
     */
    private double nmsThreshold = 0.45;

    /**
     * Maximum number of detections.
     */
    private int maxDetections = 100;

    /**
     * Enable automatic model download.
     */
    private boolean enableAutoDownload = true;

    /**
     * Download timeout in seconds.
     */
    private int downloadTimeoutSeconds = 300;

    /**
     * Input size for YOLO model (typically 640).
     */
    private int inputSize = 640;

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
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

    public double getNmsThreshold() {
        return nmsThreshold;
    }

    public void setNmsThreshold(double nmsThreshold) {
        this.nmsThreshold = nmsThreshold;
    }

    public int getMaxDetections() {
        return maxDetections;
    }

    public void setMaxDetections(int maxDetections) {
        this.maxDetections = maxDetections;
    }

    public boolean isEnableAutoDownload() {
        return enableAutoDownload;
    }

    public void setEnableAutoDownload(boolean enableAutoDownload) {
        this.enableAutoDownload = enableAutoDownload;
    }

    public int getDownloadTimeoutSeconds() {
        return downloadTimeoutSeconds;
    }

    public void setDownloadTimeoutSeconds(int downloadTimeoutSeconds) {
        this.downloadTimeoutSeconds = downloadTimeoutSeconds;
    }

    public int getInputSize() {
        return inputSize;
    }

    public void setInputSize(int inputSize) {
        this.inputSize = inputSize;
    }
}

