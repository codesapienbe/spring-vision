package io.github.codesapienbe.springvision.vllm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for Spring Vision vLLM module.
 *
 * @param enabled             whether vLLM backend is enabled
 * @param baseUrl             base URL of the vLLM server
 * @param model               model identifier (e.g., ibm-granite/granite-3.2-8b-instruct)
 * @param timeout             timeout duration for API requests
 * @param maxTokens           maximum tokens to generate
 * @param temperature         sampling temperature (0.0 - 2.0)
 * @param topP                top-p sampling parameter
 * @param confidenceThreshold confidence threshold for detections
 * @param enableStreaming     whether to enable streaming responses
 * @author Spring Vision Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.vision.vllm")
public record VllmProperties(
    boolean enabled,
    String baseUrl,
    String model,
    Duration timeout,
    int maxTokens,
    double temperature,
    double topP,
    double confidenceThreshold,
    boolean enableStreaming
) {
    /**
     * Default constructor with default values.
     */
    public VllmProperties() {
        this(
            false,
            "http://localhost:8000",
            "ibm-granite/granite-3.2-8b-instruct",
            Duration.ofSeconds(60),
            512,
            0.7,
            0.9,
            0.7,
            false
        );
    }
}

