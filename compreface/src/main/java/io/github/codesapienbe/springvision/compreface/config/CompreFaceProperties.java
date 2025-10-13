package io.github.codesapienbe.springvision.compreface.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Spring Vision CompreFace module.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
@Component
@ConfigurationProperties(prefix = "spring.vision.compreface")
public record CompreFaceProperties(
    boolean enabled,
    String apiUrl,
    String baseUrl,
    String apiKey,
    int connectTimeout,
    int readTimeout,
    int timeout,
    double detectionThreshold,
    double verificationThreshold
) {
    /**
     * Default constructor with default values.
     */
    public CompreFaceProperties() {
        this(
            true,
            "http://localhost:8000",
            "http://localhost:8000",
            "",
            5000,
            30000,
            30,
            0.7,
            0.7
        );
    }
}
