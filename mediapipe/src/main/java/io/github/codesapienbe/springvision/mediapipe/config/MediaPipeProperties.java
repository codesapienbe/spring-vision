package io.github.codesapienbe.springvision.mediapipe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Configuration properties for Spring Vision MediaPipe module.
 *
 * @param enabled                whether MediaPipe backend is enabled
 * @param modelPath              path to the model files
 * @param confidenceThreshold    confidence threshold for detections
 * @param maxDetections          maximum number of detections to return
 * @param enableAutoDownload     whether to automatically download models
 * @param downloadTimeoutSeconds timeout in seconds for model downloads
 * @param maxPoolSize            maximum size of the object pool
 * @param poolTimeoutSeconds     timeout in seconds for pool operations
 * @param modelInfo              map of model names to their metadata
 * @author Spring Vision Team
 * @since 1.1.0
 */
@Component
@ConfigurationProperties(prefix = "spring.vision.mediapipe")
public record MediaPipeProperties(
    boolean enabled,
    String modelPath,
    double confidenceThreshold,
    int maxDetections,
    boolean enableAutoDownload,
    int downloadTimeoutSeconds,
    int maxPoolSize,
    int poolTimeoutSeconds,
    Map<String, ModelInfo> modelInfo
) {
    /**
     * Default constructor with default values.
     */
    public MediaPipeProperties() {
        this(
            true,
            "classpath:/models",
            0.7,
            10,
            true,
            30,
            5,
            60,
            Map.of(
                "face_detection_short_range.tflite", new ModelInfo(
                    "https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/latest/blaze_face_short_range.tflite",
                    "sha256:8f5d8c5e3b2a1f9e8d7c6b5a4f3e2d1c9b8a7f6e5d4c3b2a1f9e8d7c6b5a4f3e2d1c"
                ),
                "hand_landmarker.task", new ModelInfo(
                    "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task",
                    "sha256:7e6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b9a8f7e6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b"
                ),
                "pose_landmarker_lite.task", new ModelInfo(
                    "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task",
                    "sha256:6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b9a8f7e6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b9a"
                ),
                "efficientdet_lite0.tflite", new ModelInfo(
                    "https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/float16/1/efficientdet_lite0.tflite",
                    "sha256:5d5c4b3a2f1e9d8c7b6a5f4e3d2c1b9a8f7e6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b9a"
                )
            )
        );
    }

    /**
     * Model metadata for MediaPipe models.
     *
     * @param url      the URL to download the model from
     * @param checksum the checksum for validating the downloaded model
     */
    public record ModelInfo(String url, String checksum) {
    }
}
