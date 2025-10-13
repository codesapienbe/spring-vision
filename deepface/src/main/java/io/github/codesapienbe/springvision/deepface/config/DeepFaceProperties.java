package io.github.codesapienbe.springvision.deepface.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration properties for Spring Vision DeepFace module.
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "spring.vision.deepface")
public record DeepFaceProperties(
    boolean enabled,
    String baseUrl,
    Duration timeout,
    double confidenceThreshold,
    int maxDetections,
    String detectorBackend,
    String model,
    String distanceMetric,
    boolean align,
    boolean normalization
) {
    /**
     * Default constructor with default values.
     */
    public DeepFaceProperties() {
        this(
            false,
            "http://localhost:5000",
            Duration.ofSeconds(30),
            0.6,
            10,
            "opencv",
            "VGG-Face",
            "cosine",
            true,
            true
        );
    }
}
