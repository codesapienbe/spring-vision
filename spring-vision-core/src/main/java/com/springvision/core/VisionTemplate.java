package com.springvision.core;

import com.springvision.core.exception.BaseVisionException;
import com.springvision.core.exception.VisionProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Template for computer vision operations providing a unified interface
 * across different vision backends (OpenCV, MediaPipe, YOLO, etc.).
 *
 * <p>This template follows the Spring template pattern, providing a
 * consistent API regardless of the underlying vision backend implementation.
 * It handles common concerns like error handling, metrics collection, and
 * result transformation.</p>
 *
 * <p>The template is designed to be used as a Spring bean and provides
 * both synchronous and asynchronous methods for vision operations. It
 * includes comprehensive logging, error handling, and performance monitoring.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @Autowired
 * private VisionTemplate visionTemplate;
 *
 * public void detectFaces(byte[] imageData) {
 *     ImageData data = ImageData.fromBytes(imageData);
 *     VisionResult result = visionTemplate.detectFaces(data);
 *     // Process VisionResult: bounding boxes, confidence, etc.
 * }
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionBackend
 * @see VisionResult
 * @see ImageData
 * @see DetectionType
 */
public class VisionTemplate {

    private static final Logger logger = LoggerFactory.getLogger(VisionTemplate.class);

    private final VisionBackend backend;

    /** Constructs a new VisionTemplate with the specified backend. */
    public VisionTemplate(VisionBackend backend) {
        this.backend = Objects.requireNonNull(backend, "Vision backend must not be null");
        logger.info("Initialized VisionTemplate with backend: {}", backend.getBackendId());
    }

    /** Gets the underlying vision backend. */
    public VisionBackend getBackend() {
        return backend;
    }

    /** Gets the backend identifier. */
    public String getBackendId() {
        return backend.getBackendId();
    }

    /** Gets the backend display name. */
    public String getBackendDisplayName() {
        return backend.getDisplayName();
    }

    /** Gets the backend version. */
    public String getBackendVersion() {
        return backend.getVersion();
    }

    /** Gets the set of detection types supported by this backend. */
    public java.util.Set<DetectionType> getSupportedDetectionTypes() {
        return backend.getSupportedDetectionTypes();
    }

    /** Checks if this backend supports a specific detection type. */
    public boolean supportsDetectionType(DetectionType detectionType) {
        return backend.supportsDetectionType(detectionType);
    }

    /** Checks if the backend is healthy. */
    public boolean isBackendHealthy() {
        return backend.isHealthy();
    }

    /** Gets detailed health information about the backend. */
    public BackendHealthInfo getBackendHealthInfo() {
        return backend.getHealthInfo();
    }

    /** Detects faces in the provided image data. */
    public VisionResult detectFaces(ImageData imageData) throws BaseVisionException {
        return detect(imageData, DetectionType.FACE);
    }

    /** Detects faces in the provided byte array. */
    public VisionResult detectFaces(byte[] imageBytes) throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detectFaces(imageData);
    }

    /** Detects objects in the provided image data. */
    public VisionResult detectObjects(ImageData imageData) throws BaseVisionException {
        return detect(imageData, DetectionType.OBJECT);
    }

    /** Detects objects in the provided byte array. */
    public VisionResult detectObjects(byte[] imageBytes) throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detectObjects(imageData);
    }

    /** Performs a generic detection operation based on the specified detection type. */
    public VisionResult detect(ImageData imageData, DetectionType detectionType) throws BaseVisionException {
        Objects.requireNonNull(imageData, "Image data must not be null");
        Objects.requireNonNull(detectionType, "Detection type must not be null");

        if (!supportsDetectionType(detectionType)) {
            throw new IllegalArgumentException(
                String.format("Detection type '%s' is not supported by backend '%s'",
                    detectionType.getDisplayName(), getBackendId()));
        }

        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        logger.info("Starting {} detection", Map.of(
            "correlationId", correlationId,
            "detectionType", detectionType.getDisplayName(),
            "imageSize", imageData.getSizeInBytes(),
            "imageFormat", imageData.format(),
            "backendId", getBackendId()
        ));

        try {
            VisionResult result = backend.detect(imageData, detectionType);
            long processingTime = System.currentTimeMillis() - startTime;

            logger.info("{} detection completed", Map.of(
                "correlationId", correlationId,
                "detectionType", detectionType.getDisplayName(),
                "detectionsFound", result.detectionCount(),
                "averageConfidence", result.averageConfidence(),
                "processingTimeMs", processingTime,
                "backendId", getBackendId()
            ));

            return result;

        } catch (BaseVisionException e) {
            long processingTime = System.currentTimeMillis() - startTime;

            logger.error("{} detection failed", Map.of(
                "correlationId", correlationId,
                "detectionType", detectionType.getDisplayName(),
                "processingTimeMs", processingTime,
                "backendId", getBackendId(),
                "error", e.getClass().getSimpleName()
            ), e);

            throw e;

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;

            logger.error("Unexpected error during {} detection", Map.of(
                "correlationId", correlationId,
                "detectionType", detectionType.getDisplayName(),
                "processingTimeMs", processingTime,
                "backendId", getBackendId(),
                "error", e.getClass().getSimpleName()
            ), e);

            throw new VisionProcessingException(
                String.format("Unexpected error during %s detection", detectionType.getDisplayName()),
                detectionType.getDisplayName().toLowerCase(),
                detectionType.getCode(),
                e
            );
        }
    }

    /** Performs a generic detection operation on byte array data. */
    public VisionResult detect(byte[] imageBytes, DetectionType detectionType) throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detect(imageData, detectionType);
    }

    /** Extracts face embeddings using the backend's implementation or default support. */
    public List<float[]> extractEmbeddings(ImageData imageData) throws BaseVisionException {
        Objects.requireNonNull(imageData, "Image data must not be null");
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();
        logger.info("Starting embedding extraction", Map.of(
            "correlationId", correlationId,
            "imageSize", imageData.getSizeInBytes(),
            "imageFormat", imageData.format(),
            "backendId", getBackendId()
        ));
        List<float[]> embeddings = backend.extractEmbeddings(imageData);
        long processingTime = System.currentTimeMillis() - startTime;
        logger.info("Embedding extraction completed", Map.of(
            "correlationId", correlationId,
            "embeddingsCount", embeddings == null ? 0 : embeddings.size(),
            "processingTimeMs", processingTime,
            "backendId", getBackendId()
        ));
        return embeddings;
    }

    /** Verifies whether two images belong to the same identity. */
    public boolean verify(ImageData a, ImageData b, String metric, double threshold) throws BaseVisionException {
        Objects.requireNonNull(a, "First image must not be null");
        Objects.requireNonNull(b, "Second image must not be null");
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();
        logger.info("Starting verification", Map.of(
            "correlationId", correlationId,
            "metric", metric,
            "threshold", threshold,
            "backendId", getBackendId()
        ));
        boolean result = backend.verify(a, b, metric, threshold);
        long processingTime = System.currentTimeMillis() - startTime;
        logger.info("Verification completed", Map.of(
            "correlationId", correlationId,
            "isMatch", result,
            "processingTimeMs", processingTime,
            "backendId", getBackendId()
        ));
        return result;
    }

    /** Generates a unique correlation ID for tracking operations. */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
