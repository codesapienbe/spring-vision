package io.github.codesapienbe.springvision.facebytes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Vision FaceBytes module.
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.vision.facebytes")
public class FaceBytesProperties {

    /**
     * Enable/disable FaceBytes backend.
     */
    private boolean enabled = false;

    /**
     * Model path for FaceBytes models.
     */
    private String modelPath = "~/.spring-vision/models/facebytes";

    /**
     * Face detection backend (opencv, dlib, retinaface, etc.).
     */
    private String detectorBackend = "opencv";

    /**
     * Face recognition model (VGG-Face, Facenet, OpenFace, DeepFace, etc.).
     */
    private String recognitionModel = "VGG-Face";

    /**
     * Distance metric for face comparison (cosine, euclidean, euclidean_l2).
     */
    private String distanceMetric = "cosine";

    /**
     * Confidence threshold for face detection (0.0 - 1.0).
     */
    private double confidenceThreshold = 0.7;

    /**
     * Maximum number of faces to detect.
     */
    private int maxDetections = 10;

    /**
     * Enable automatic model download.
     */
    private boolean enableAutoDownload = true;

    /**
     * Enable face alignment.
     */
    private boolean enableAlignment = true;

    /**
     * Enable face quality validation.
     */
    private boolean enableQualityCheck = false;

    /**
     * Minimum face size for detection (pixels).
     */
    private int minFaceSize = 20;

    // Getters and Setters

    /**
     * Checks if FaceBytes is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the enabled status for FaceBytes.
     *
     * @param enabled the enabled status to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the model path.
     *
     * @return the model path
     */
    public String getModelPath() {
        return modelPath;
    }

    /**
     * Sets the model path.
     *
     * @param modelPath the model path to set
     */
    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    /**
     * Gets the detector backend.
     *
     * @return the detector backend name
     */
    public String getDetectorBackend() {
        return detectorBackend;
    }

    /**
     * Sets the detector backend.
     *
     * @param detectorBackend the detector backend to set
     */
    public void setDetectorBackend(String detectorBackend) {
        this.detectorBackend = detectorBackend;
    }

    /**
     * Gets the recognition model.
     *
     * @return the recognition model name
     */
    public String getRecognitionModel() {
        return recognitionModel;
    }

    /**
     * Sets the recognition model.
     *
     * @param recognitionModel the recognition model to set
     */
    public void setRecognitionModel(String recognitionModel) {
        this.recognitionModel = recognitionModel;
    }

    /**
     * Gets the distance metric.
     *
     * @return the distance metric name
     */
    public String getDistanceMetric() {
        return distanceMetric;
    }

    /**
     * Sets the distance metric.
     *
     * @param distanceMetric the distance metric to set
     */
    public void setDistanceMetric(String distanceMetric) {
        this.distanceMetric = distanceMetric;
    }

    /**
     * Gets the confidence threshold.
     *
     * @return the confidence threshold
     */
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    /**
     * Sets the confidence threshold.
     *
     * @param confidenceThreshold the confidence threshold to set
     */
    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    /**
     * Gets the maximum number of detections.
     *
     * @return the max detections
     */
    public int getMaxDetections() {
        return maxDetections;
    }

    /**
     * Sets the maximum number of detections.
     *
     * @param maxDetections the max detections to set
     */
    public void setMaxDetections(int maxDetections) {
        this.maxDetections = maxDetections;
    }

    /**
     * Checks if automatic model download is enabled.
     *
     * @return true if auto-download is enabled, false otherwise
     */
    public boolean isEnableAutoDownload() {
        return enableAutoDownload;
    }

    /**
     * Sets the automatic model download status.
     *
     * @param enableAutoDownload the auto-download status to set
     */
    public void setEnableAutoDownload(boolean enableAutoDownload) {
        this.enableAutoDownload = enableAutoDownload;
    }

    /**
     * Checks if face alignment is enabled.
     *
     * @return true if alignment is enabled, false otherwise
     */
    public boolean isEnableAlignment() {
        return enableAlignment;
    }

    /**
     * Sets the face alignment status.
     *
     * @param enableAlignment the alignment status to set
     */
    public void setEnableAlignment(boolean enableAlignment) {
        this.enableAlignment = enableAlignment;
    }

    /**
     * Checks if face quality validation is enabled.
     *
     * @return true if quality check is enabled, false otherwise
     */
    public boolean isEnableQualityCheck() {
        return enableQualityCheck;
    }

    /**
     * Sets the face quality validation status.
     *
     * @param enableQualityCheck the quality check status to set
     */
    public void setEnableQualityCheck(boolean enableQualityCheck) {
        this.enableQualityCheck = enableQualityCheck;
    }

    /**
     * Gets the minimum face size.
     *
     * @return the minimum face size in pixels
     */
    public int getMinFaceSize() {
        return minFaceSize;
    }

    /**
     * Sets the minimum face size.
     *
     * @param minFaceSize the minimum face size to set
     */
    public void setMinFaceSize(int minFaceSize) {
        this.minFaceSize = minFaceSize;
    }
}

