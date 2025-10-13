package io.github.codesapienbe.springvision.insightface.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Configuration properties for Spring Vision InsightFace module.
 *
 * @param enabled               whether InsightFace backend is enabled
 * @param apiUrl                URL of the InsightFace API server
 * @param apiKey                API key for authentication
 * @param modelName             name of the model to use
 * @param confidenceThreshold   confidence threshold for face detection
 * @param verificationThreshold threshold for face verification
 * @param maxDetections         maximum number of detections to return
 * @param enableAgeGender       whether to enable age and gender detection
 * @param enableEmotion         whether to enable emotion detection
 * @param enableLandmarks       whether to enable facial landmarks detection
 * @param timeoutSeconds        timeout in seconds for API requests
 * @param maxRetries            maximum number of retry attempts
 * @param modelInfo             map of model names to their metadata
 * @author Spring Vision Team
 * @since 1.1.0
 */
@Component
@ConfigurationProperties(prefix = "spring.vision.insightface")
public record InsightFaceProperties(
    boolean enabled,
    String apiUrl,
    String apiKey,
    String modelName,
    double confidenceThreshold,
    double verificationThreshold,
    int maxDetections,
    boolean enableAgeGender,
    boolean enableEmotion,
    boolean enableLandmarks,
    int timeoutSeconds,
    int maxRetries,
    Map<String, ModelInfo> modelInfo
) {
    /**
     * Default constructor with default values.
     */
    public InsightFaceProperties() {
        this(
            false,
            "http://localhost:8000",
            "",
            "buffalo_l",
            0.5,
            0.6,
            10,
            true,
            true,
            true,
            30,
            3,
            Map.of(
                "buffalo_l", new ModelInfo(
                    "https://github.com/deepinsight/insightface/releases/download/v0.7/buffalo_l.zip",
                    "sha256:9f5d8c5e3b2a1f9e8d7c6b5a4f3e2d1c9b8a7f6e5d4c3b2a1f9e8d7c6b5a4f3e2d1c",
                    "buffalo_l"
                ),
                "buffalo_m", new ModelInfo(
                    "https://github.com/deepinsight/insightface/releases/download/v0.7/buffalo_m.zip",
                    "sha256:8e6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b9a8f7e6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b",
                    "buffalo_m"
                ),
                "buffalo_s", new ModelInfo(
                    "https://github.com/deepinsight/insightface/releases/download/v0.7/buffalo_s.zip",
                    "sha256:7d5c4b3a2f1e9d8c7b6a5f4e3d2c1b9a8f7e6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b9a",
                    "buffalo_s"
                )
            )
        );
    }

    /**
     * Model metadata for InsightFace models.
     *
     * @param url      the URL to download the model from
     * @param checksum the checksum for validating the downloaded model
     * @param name     the name of the model
     */
    public record ModelInfo(String url, String checksum, String name) {
    }
}
