package io.github.codesapienbe.springvision.facebytes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Spring Vision FaceBytes module.
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "spring.vision.facebytes")
public record FaceBytesProperties(
    boolean enabled,
    String modelPath,
    String detectorBackend,
    String recognitionModel,
    String distanceMetric,
    double confidenceThreshold,
    int maxDetections,
    boolean enableAutoDownload,
    boolean enableAlignment,
    boolean enableQualityCheck,
    int minFaceSize
) {
    /**
     * Default constructor with default values.
     */
    public FaceBytesProperties() {
        this(
            false,
            "classpath:/models",
            "opencv",
            "VGG-Face",
            "cosine",
            0.7,
            10,
            true,
            true,
            false,
            20
        );
    }
}
