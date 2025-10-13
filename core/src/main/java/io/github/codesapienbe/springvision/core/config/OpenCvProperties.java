package io.github.codesapienbe.springvision.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for OpenCV Vision Backend.
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Component
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

