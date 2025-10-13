package io.github.codesapienbe.springvision.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OpenCV Vision Backend.
 *
 * @param enabled                whether OpenCV backend is enabled
 * @param confidenceThreshold    confidence threshold for detections
 * @param maxDetections          maximum number of detections to return
 * @param enableAutoDownload     whether to automatically download models
 * @param downloadTimeoutSeconds timeout in seconds for model downloads
 * @param modelPath              path to the model files
 * @param maxPoolSize            maximum size of the object pool
 * @param poolTimeoutSeconds     timeout in seconds for pool operations
 * @author Spring Vision Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.vision.opencv")
public record OpenCvProperties(
    boolean enabled,
    double confidenceThreshold,
    int maxDetections,
    boolean enableAutoDownload,
    int downloadTimeoutSeconds,
    String modelPath,
    int maxPoolSize,
    int poolTimeoutSeconds
) {
    /**
     * Default constructor with default values.
     */
    public OpenCvProperties() {
        this(
            true,
            0.8,
            10,
            true,
            30,
            "classpath:/models",
            5,
            60
        );
    }
}
