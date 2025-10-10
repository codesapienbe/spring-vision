package io.github.codesapienbe.springvision.mediapipe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Vision MediaPipe module.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.vision.mediapipe")
public class MediaPipeProperties {

    /**
     * Whether MediaPipe backend is enabled.
     */
    private boolean enabled = true;

    /**
     * Model complexity for face detection (0, 1, or 2).
     * Higher values mean more accurate but slower detection.
     */
    private int modelComplexity = 1;

    /**
     * Minimum detection confidence threshold (0.0 to 1.0).
     */
    private float minDetectionConfidence = 0.5f;

    /**
     * Minimum tracking confidence threshold (0.0 to 1.0).
     */
    private float minTrackingConfidence = 0.5f;

    /**
     * Gets whether MediaPipe backend is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether MediaPipe backend is enabled.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the model complexity.
     *
     * @return model complexity value
     */
    public int getModelComplexity() {
        return modelComplexity;
    }

    /**
     * Sets the model complexity.
     *
     * @param modelComplexity model complexity value
     */
    public void setModelComplexity(int modelComplexity) {
        this.modelComplexity = modelComplexity;
    }

    /**
     * Gets the minimum detection confidence.
     *
     * @return minimum detection confidence
     */
    public float getMinDetectionConfidence() {
        return minDetectionConfidence;
    }

    /**
     * Sets the minimum detection confidence.
     *
     * @param minDetectionConfidence minimum detection confidence
     */
    public void setMinDetectionConfidence(float minDetectionConfidence) {
        this.minDetectionConfidence = minDetectionConfidence;
    }

    /**
     * Gets the minimum tracking confidence.
     *
     * @return minimum tracking confidence
     */
    public float getMinTrackingConfidence() {
        return minTrackingConfidence;
    }

    /**
     * Sets the minimum tracking confidence.
     *
     * @param minTrackingConfidence minimum tracking confidence
     */
    public void setMinTrackingConfidence(float minTrackingConfidence) {
        this.minTrackingConfidence = minTrackingConfidence;
    }
}

