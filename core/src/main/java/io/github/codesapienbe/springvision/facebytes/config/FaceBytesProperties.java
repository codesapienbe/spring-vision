package io.github.codesapienbe.springvision.facebytes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Vision FaceBytes module.
 *
 * @param enabled             whether FaceBytes backend is enabled
 * @param modelPath           path to the model files
 * @param detectorBackend     detector backend to use (e.g., 'opencv', 'ssd', 'dlib')
 * @param recognitionModel    face recognition model to use
 * @param distanceMetric      distance metric for face comparison
 * @param confidenceThreshold confidence threshold for detections
 * @param maxDetections       maximum number of detections to return
 * @param enableAutoDownload  whether to automatically download models
 * @param enableAlignment     whether to align faces before processing
 * @param enableQualityCheck  whether to perform quality checks on detected faces
 * @param minFaceSize         minimum face size in pixels
 * @author Spring Vision Team
 * @since 1.0.0
 */
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
