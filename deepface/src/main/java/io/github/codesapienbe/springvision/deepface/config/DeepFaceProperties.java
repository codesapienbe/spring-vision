package io.github.codesapienbe.springvision.deepface.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration properties for Spring Vision DeepFace module.
 *
 * @param enabled             whether DeepFace backend is enabled
 * @param baseUrl             base URL of the DeepFace server
 * @param timeout             timeout duration for API requests
 * @param confidenceThreshold confidence threshold for detections
 * @param maxDetections       maximum number of detections to return
 * @param detectorBackend     detector backend to use
 * @param model               face recognition model to use
 * @param distanceMetric      distance metric for face comparison
 * @param align               whether to align faces before processing
 * @param normalization       whether to apply normalization
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
