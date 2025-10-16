package io.github.codesapienbe.springvision.core.djl;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for DJL vision backend.
 *
 * @author Spring Vision Team
 * @since 1.0.5
 */
@ConfigurationProperties(prefix = "spring.vision.djl")
public class DjlProperties {

    /**
     * Whether DJL backend is enabled.
     */
    private boolean enabled = false;

    /**
     * Preferred engine (PyTorch, OnnxRuntime, TensorFlow, MXNet).
     */
    private String engine = "PyTorch";

    /**
     * Model cache directory. Defaults to ~/.djl.ai
     */
    private String cacheDir;

    /**
     * Face detection model URL or path.
     */
    private String faceDetectionModel;

    /**
     * Face recognition model URL or path.
     */
    private String faceRecognitionModel;

    /**
     * Object detection model URL, artifact ID, or local path.
     */
    private String objectDetectionModel;

    /**
     * Whether to use GPU acceleration when available.
     */
    private boolean useGpu = false;

    /**
     * Model zoo location (comma-separated URLs).
     */
    private String modelZooLocation;

    /**
     * Minimum confidence threshold for detections.
     */
    private double confidenceThreshold = 0.5;

    /**
     * Maximum number of concurrent inference threads.
     */
    private int maxConcurrentInferences = 4;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public String getFaceDetectionModel() {
        return faceDetectionModel;
    }

    public void setFaceDetectionModel(String faceDetectionModel) {
        this.faceDetectionModel = faceDetectionModel;
    }

    public String getFaceRecognitionModel() {
        return faceRecognitionModel;
    }

    public void setFaceRecognitionModel(String faceRecognitionModel) {
        this.faceRecognitionModel = faceRecognitionModel;
    }

    public String getObjectDetectionModel() {
        return objectDetectionModel;
    }

    public void setObjectDetectionModel(String objectDetectionModel) {
        this.objectDetectionModel = objectDetectionModel;
    }

    public boolean isUseGpu() {
        return useGpu;
    }

    public void setUseGpu(boolean useGpu) {
        this.useGpu = useGpu;
    }

    public String getModelZooLocation() {
        return modelZooLocation;
    }

    public void setModelZooLocation(String modelZooLocation) {
        this.modelZooLocation = modelZooLocation;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public int getMaxConcurrentInferences() {
        return maxConcurrentInferences;
    }

    public void setMaxConcurrentInferences(int maxConcurrentInferences) {
        this.maxConcurrentInferences = maxConcurrentInferences;
    }
}
