package io.github.codesapienbe.springvision.compreface.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Spring Vision CompreFace module.
 *
 * @param enabled               whether CompreFace backend is enabled
 * @param apiUrl                URL of the CompreFace API server
 * @param baseUrl               base URL of the CompreFace server
 * @param apiKey                API key for authentication
 * @param connectTimeout        connection timeout in milliseconds
 * @param readTimeout           read timeout in milliseconds
 * @param timeout               general timeout in seconds
 * @param detectionThreshold    threshold for face detection
 * @param verificationThreshold threshold for face verification
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
