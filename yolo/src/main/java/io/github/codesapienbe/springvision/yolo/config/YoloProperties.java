package io.github.codesapienbe.springvision.yolo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Configuration properties for Spring Vision YOLO module.
 *
 * @param enabled                whether YOLO backend is enabled
 * @param modelPath              path to the model files
 * @param modelName              name of the YOLO model to use
 * @param confidenceThreshold    confidence threshold for detections
 * @param nmsThreshold           non-maximum suppression threshold
 * @param maxDetections          maximum number of detections to return
 * @param enableAutoDownload     whether to automatically download models
 * @param downloadTimeoutSeconds timeout in seconds for model downloads
 * @param inputSize              input size for the model
 * @param modelInfo              map of model names to their metadata
 * @author Spring Vision Team
 * @since 1.1.0
 */
@Component
@ConfigurationProperties(prefix = "spring.vision.yolo")
public record YoloProperties(
    boolean enabled,
    String modelPath,
    String modelName,
    double confidenceThreshold,
    double nmsThreshold,
    int maxDetections,
    boolean enableAutoDownload,
    int downloadTimeoutSeconds,
    int inputSize,
    Map<String, ModelInfo> modelInfo
) {
    /**
     * Default constructor with default values.
     */
    public YoloProperties() {
        this(
            false,
            "classpath:/models",
            "yolov8n.onnx",
            0.25,
            0.45,
            100,
            true,
            300,
            640,
            Map.of(
                "yolov8n.onnx", new ModelInfo(
                    "https://github.com/ultralytics/assets/releases/download/v0.0.0/yolov8n.pt",
                    "sha256:6fcc2a971d8bc901e81db872e3c01dd6357d11ac502b4bed4c78ddc2c5d47d6a",
                    "yolov8n"
                ),
                "yolov8s.onnx", new ModelInfo(
                    "https://github.com/ultralytics/assets/releases/download/v0.0.0/yolov8s.pt",
                    "sha256:a3ec3c53f073fd53f22e8cb7e75a4c7d3c07a1ca40b621cce04c175652206572",
                    "yolov8s"
                ),
                "yolov8m.onnx", new ModelInfo(
                    "https://github.com/ultralytics/assets/releases/download/v0.0.0/yolov8m.pt",
                    "sha256:c2ce8e0240d84c5c7b8b4d8f21de54e9c71e8e0b5b8b4d8f21de54e9c71e8e0b",
                    "yolov8m"
                )
            )
        );
    }

    /**
     * Model metadata for YOLO models.
     *
     * @param url      the URL to download the model from
     * @param checksum the checksum for validating the downloaded model
     * @param name     the name of the model
     */
    public record ModelInfo(String url, String checksum, String name) {
    }
}
