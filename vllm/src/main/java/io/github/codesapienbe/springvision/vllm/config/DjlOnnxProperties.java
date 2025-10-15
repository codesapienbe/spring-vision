package io.github.codesapienbe.springvision.vllm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for DJL-based ONNX Runtime backend.
 *
 * @param enabled             whether DJL ONNX backend is enabled
 * @param modelPath           path to ONNX model file
 * @param executionProvider   execution provider (OpenVINO, CUDA, TensorRT, CPU)
 * @param inputSize           input image size (e.g., 224 for SqueezeNet)
 * @param confidenceThreshold confidence threshold for detections
 * @author Spring Vision Team
 * @since 1.0.3
 */
@ConfigurationProperties(prefix = "spring.vision.vllm.embedded.djl")
public record DjlOnnxProperties(
    boolean enabled,
    String modelPath,
    String executionProvider,
    int inputSize,
    double confidenceThreshold
) {
    /**
     * Default constructor with default values.
     */
    public DjlOnnxProperties() {
        this(
            false,
            "models/squeezenet_int8.onnx",
            "OpenVINO",
            224,
            0.7
        );
    }
}

