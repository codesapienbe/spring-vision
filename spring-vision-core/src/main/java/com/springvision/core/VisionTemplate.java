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

    /**
     * Constructs a new VisionTemplate with the specified backend.
     *
     * @param backend the vision backend to use for operations
     * @throws IllegalArgumentException if backend is null
     */
    public VisionTemplate(VisionBackend backend) {
        this.backend = Objects.requireNonNull(backend, "Vision backend must not be null");
        logger.info("Initialized VisionTemplate with backend: {}", backend.getBackendId());
    }

    /**
     * Gets the underlying vision backend.
     *
     * @return the vision backend
     */
    public VisionBackend getBackend() {
        return backend;
    }

    /**
     * Gets the backend identifier.
     *
     * @return the backend identifier
     */
    public String getBackendId() {
        return backend.getBackendId();
    }

    /**
     * Gets the backend display name.
     *
     * @return the backend display name
     */
    public String getBackendDisplayName() {
        return backend.getDisplayName();
    }

    /**
     * Gets the backend version.
     *
     * @return the backend version
     */
    public String getBackendVersion() {
        return backend.getVersion();
    }

    /**
     * Gets the set of detection types supported by this backend.
     *
     * @return the set of supported detection types
     */
    public java.util.Set<DetectionType> getSupportedDetectionTypes() {
        return backend.getSupportedDetectionTypes();
    }

    /**
     * Checks if this backend supports a specific detection type.
     *
     * @param detectionType the detection type to check
     * @return true if the detection type is supported, false otherwise
     */
    public boolean supportsDetectionType(DetectionType detectionType) {
        return backend.supportsDetectionType(detectionType);
    }

    /**
     * Checks if the backend is healthy.
     *
     * @return true if the backend is healthy, false otherwise
     */
    public boolean isBackendHealthy() {
        return backend.isHealthy();
    }

    /**
     * Gets detailed health information about the backend.
     *
     * @return detailed health information
     */
    public BackendHealthInfo getBackendHealthInfo() {
        return backend.getHealthInfo();
    }

    /**
     * Detects faces in the provided image data.
     *
     * <p>This method processes the image using the configured vision backend
     * and returns detection results including bounding boxes and confidence
     * scores for each detected face.</p>
     *
     * @param imageData the image data to process, must not be null
     * @return a {@link VisionResult} containing face detection results
     * @throws IllegalArgumentException if imageData is null or empty
     * @throws BaseVisionException if the vision backend fails to process the image
     * @throws SecurityException if the image fails security validation
     */
    public VisionResult detectFaces(ImageData imageData) throws BaseVisionException {
        return detect(imageData, DetectionType.FACE);
    }

    /**
     * Detects faces in the provided byte array.
     *
     * <p>This convenience method creates an ImageData object from the byte array
     * and then performs face detection.</p>
     *
     * @param imageBytes the image data as a byte array, must not be null
     * @return a {@link VisionResult} containing face detection results
     * @throws IllegalArgumentException if imageBytes is null or empty
     * @throws BaseVisionException if the vision backend fails to process the image
     * @throws SecurityException if the image fails security validation
     */
    public VisionResult detectFaces(byte[] imageBytes) throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detectFaces(imageData);
    }

    /**
     * Detects objects in the provided image data.
     *
     * <p>This method detects various objects in the image and returns detection
     * results including bounding boxes, class labels, and confidence scores.</p>
     *
     * @param imageData the image data to process, must not be null
     * @return a {@link VisionResult} containing object detection results
     * @throws IllegalArgumentException if imageData is null or empty
     * @throws BaseVisionException if the vision backend fails to process the image
     * @throws SecurityException if the image fails security validation
     */
    public VisionResult detectObjects(ImageData imageData) throws BaseVisionException {
        return detect(imageData, DetectionType.OBJECT);
    }

    /**
     * Detects objects in the provided byte array.
     *
     * @param imageBytes the image data as a byte array, must not be null
     * @return a {@link VisionResult} containing object detection results
     * @throws IllegalArgumentException if imageBytes is null or empty
     * @throws BaseVisionException if the vision backend fails to process the image
     * @throws SecurityException if the image fails security validation
     */
    public VisionResult detectObjects(byte[] imageBytes) throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detectObjects(imageData);
    }

    /**
     * Performs a generic detection operation based on the specified detection type.
     *
     * <p>This method provides a unified interface for all detection types.
     * It includes comprehensive logging, error handling, and performance monitoring.</p>
     *
     * @param imageData the image data to process, must not be null
     * @param detectionType the type of detection to perform, must not be null
     * @return the detection results
     * @throws IllegalArgumentException if imageData is null, detectionType is null, or detection type is not supported
     * @throws BaseVisionException if the vision backend fails to process the image
     * @throws SecurityException if the image fails security validation
     */
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

    /**
     * Performs a generic detection operation on byte array data.
     *
     * @param imageBytes the image data as a byte array, must not be null
     * @param detectionType the type of detection to perform, must not be null
     * @return the detection results
     * @throws IllegalArgumentException if imageBytes is null, detectionType is null, or detection type is not supported
     * @throws BaseVisionException if the vision backend fails to process the image
     * @throws SecurityException if the image fails security validation
     */
    public VisionResult detect(byte[] imageBytes, DetectionType detectionType) throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detect(imageData, detectionType);
    }

    /**
     * Performs multiple detection types on the provided image data.
     *
     * <p>This method allows performing multiple detection operations in a single
     * call, which can be more efficient than multiple individual calls.</p>
     *
     * @param imageData the image data to process, must not be null
     * @param detectionTypes the types of detection to perform, must not be null or empty
     * @return a list of detection results in the same order as the detection types
     * @throws IllegalArgumentException if imageData is null, detectionTypes is null/empty, or any detection type is not supported
     * @throws BaseVisionException if any detection fails
     * @throws SecurityException if the image fails security validation
     */
    public List<VisionResult> detectMultiple(ImageData imageData, List<DetectionType> detectionTypes)
            throws BaseVisionException {
        Objects.requireNonNull(imageData, "Image data must not be null");
        Objects.requireNonNull(detectionTypes, "Detection types must not be null");

        if (detectionTypes.isEmpty()) {
            throw new IllegalArgumentException("Detection types must not be empty");
        }

        // Validate all detection types are supported
        for (DetectionType detectionType : detectionTypes) {
            if (!supportsDetectionType(detectionType)) {
                throw new IllegalArgumentException(
                    String.format("Detection type '%s' is not supported by backend '%s'",
                        detectionType.getDisplayName(), getBackendId()));
            }
        }

        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        logger.info("Starting multiple detections", Map.of(
            "correlationId", correlationId,
            "detectionTypes", detectionTypes.stream().map(DetectionType::getDisplayName).toList(),
            "imageSize", imageData.getSizeInBytes(),
            "imageFormat", imageData.format(),
            "backendId", getBackendId()
        ));

        try {
            List<VisionResult> results = backend.detectMultiple(imageData, detectionTypes);
            long processingTime = System.currentTimeMillis() - startTime;

            logger.info("Multiple detections completed", Map.of(
                "correlationId", correlationId,
                "detectionTypes", detectionTypes.stream().map(DetectionType::getDisplayName).toList(),
                "totalDetections", results.stream().mapToInt(VisionResult::detectionCount).sum(),
                "processingTimeMs", processingTime,
                "backendId", getBackendId()
            ));

            return results;

        } catch (BaseVisionException e) {
            long processingTime = System.currentTimeMillis() - startTime;

            logger.error("Multiple detections failed", Map.of(
                "correlationId", correlationId,
                "detectionTypes", detectionTypes.stream().map(DetectionType::getDisplayName).toList(),
                "processingTimeMs", processingTime,
                "backendId", getBackendId(),
                "error", e.getClass().getSimpleName()
            ), e);

            throw e;
        }
    }

    /**
     * Performs multiple detection types on byte array data.
     *
     * @param imageBytes the image data as a byte array, must not be null
     * @param detectionTypes the types of detection to perform, must not be null or empty
     * @return a list of detection results in the same order as the detection types
     * @throws IllegalArgumentException if imageBytes is null, detectionTypes is null/empty, or any detection type is not supported
     * @throws BaseVisionException if any detection fails
     * @throws SecurityException if the image fails security validation
     */
    public List<VisionResult> detectMultiple(byte[] imageBytes, List<DetectionType> detectionTypes)
            throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detectMultiple(imageData, detectionTypes);
    }

    /**
     * Generates a unique correlation ID for tracking operations.
     *
     * @return a unique correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
