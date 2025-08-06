package com.springvision.core;

import java.util.List;
import java.util.Set;

import com.springvision.core.exception.BaseVisionException;

/**
 * Service Provider Interface for computer vision backends.
 *
 * <p>This interface defines the contract that all vision backends must implement
 * to integrate with the Spring Vision framework. It provides a unified API for
 * performing various computer vision operations regardless of the underlying
 * implementation (OpenCV, MediaPipe, YOLO, etc.).</p>
 *
 * <p>The interface supports multiple detection types and provides methods for
 * checking backend capabilities, health status, and performing detections.
 * Implementations should be thread-safe and handle errors gracefully.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Get available backends
 * ServiceLoader<VisionBackend> backends = ServiceLoader.load(VisionBackend.class);
 *
 * // Use a specific backend
 * VisionBackend opencvBackend = // ... get OpenCV backend
 * VisionResult result = opencvBackend.detectFaces(imageData);
 * }</pre>
 *
 * <p>Implementations should be registered using the Java Service Provider
 * mechanism by creating a file {@code META-INF/services/com.springvision.core.VisionBackend}
 * containing the fully qualified class name of the implementation.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionTemplate
 * @see ImageData
 * @see VisionResult
 * @see DetectionType
 */
public interface VisionBackend {

    /**
     * Gets the unique identifier for this vision backend.
     *
     * <p>This identifier should be unique across all backend implementations
     * and is used for configuration, logging, and backend selection.</p>
     *
     * @return the backend identifier (e.g., "opencv", "mediapipe", "yolo")
     */
    String getBackendId();

    /**
     * Gets the display name for this vision backend.
     *
     * @return a human-readable name for the backend
     */
    String getDisplayName();

    /**
     * Gets the version of this vision backend.
     *
     * @return the backend version string
     */
    String getVersion();

    /**
     * Gets the set of detection types supported by this backend.
     *
     * <p>This method should return all detection types that this backend
     * can perform. The returned set should be immutable.</p>
     *
     * @return a set of supported detection types
     */
    Set<DetectionType> getSupportedDetectionTypes();

    /**
     * Checks if this backend supports a specific detection type.
     *
     * @param detectionType the detection type to check
     * @return true if the detection type is supported, false otherwise
     */
    default boolean supportsDetectionType(DetectionType detectionType) {
        return getSupportedDetectionTypes().contains(detectionType);
    }

    /**
     * Checks if this backend is healthy and ready to process requests.
     *
     * <p>This method should perform a lightweight health check to verify
     * that the backend is operational. It should not perform any heavy
     * operations or external calls.</p>
     *
     * @return true if the backend is healthy, false otherwise
     */
    boolean isHealthy();

    /**
     * Gets detailed health information about this backend.
     *
     * <p>This method should return comprehensive health information including
     * status, error details if unhealthy, and any relevant metrics.</p>
     *
     * @return detailed health information
     */
    BackendHealthInfo getHealthInfo();

    /**
     * Performs face detection on the provided image data.
     *
     * <p>This method detects human faces in the image and returns detection
     * results including bounding boxes and confidence scores.</p>
     *
     * @param imageData the image data to process
     * @return the face detection results
     * @throws BaseVisionException if face detection fails
     * @throws IllegalArgumentException if imageData is null or invalid
     */
    VisionResult detectFaces(ImageData imageData) throws BaseVisionException;

    /**
     * Performs object detection on the provided image data.
     *
     * <p>This method detects various objects in the image and returns detection
     * results including bounding boxes, class labels, and confidence scores.</p>
     *
     * @param imageData the image data to process
     * @return the object detection results
     * @throws BaseVisionException if object detection fails
     * @throws IllegalArgumentException if imageData is null or invalid
     */
    VisionResult detectObjects(ImageData imageData) throws BaseVisionException;

    /**
     * Performs a generic detection operation based on the specified detection type.
     *
     * <p>This method provides a unified interface for all detection types.
     * The actual implementation should delegate to the appropriate specific
     * method based on the detection type.</p>
     *
     * @param imageData the image data to process
     * @param detectionType the type of detection to perform
     * @return the detection results
     * @throws BaseVisionException if detection fails
     * @throws IllegalArgumentException if imageData is null, detectionType is null, or detection type is not supported
     */
    default VisionResult detect(ImageData imageData, DetectionType detectionType) throws BaseVisionException {
        if (imageData == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }
        if (detectionType == null) {
            throw new IllegalArgumentException("Detection type must not be null");
        }
        if (!supportsDetectionType(detectionType)) {
            throw new IllegalArgumentException(
                String.format("Detection type '%s' is not supported by backend '%s'",
                    detectionType.getDisplayName(), getBackendId()));
        }

        return switch (detectionType) {
            case FACE -> detectFaces(imageData);
            case OBJECT -> detectObjects(imageData);
            case TEXT -> detectText(imageData);
            case BARCODE -> detectBarcodes(imageData);
            case LANDMARK -> detectLandmarks(imageData);
            case POSE -> detectPoses(imageData);
            case HAND -> detectHands(imageData);
            case CUSTOM -> detectCustom(imageData);
        };
    }

    /**
     * Performs text recognition (OCR) on the provided image data.
     *
     * @param imageData the image data to process
     * @return the text recognition results
     * @throws BaseVisionException if text recognition fails
     * @throws IllegalArgumentException if imageData is null or invalid
     */
    default VisionResult detectText(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException(
            String.format("Text detection is not supported by backend '%s'", getBackendId()));
    }

    /**
     * Performs barcode/QR code detection on the provided image data.
     *
     * @param imageData the image data to process
     * @return the barcode detection results
     * @throws BaseVisionException if barcode detection fails
     * @throws IllegalArgumentException if imageData is null or invalid
     */
    default VisionResult detectBarcodes(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException(
            String.format("Barcode detection is not supported by backend '%s'", getBackendId()));
    }

    /**
     * Performs landmark detection on the provided image data.
     *
     * @param imageData the image data to process
     * @return the landmark detection results
     * @throws BaseVisionException if landmark detection fails
     * @throws IllegalArgumentException if imageData is null or invalid
     */
    default VisionResult detectLandmarks(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException(
            String.format("Landmark detection is not supported by backend '%s'", getBackendId()));
    }

    /**
     * Performs pose estimation on the provided image data.
     *
     * @param imageData the image data to process
     * @return the pose estimation results
     * @throws BaseVisionException if pose estimation fails
     * @throws IllegalArgumentException if imageData is null or invalid
     */
    default VisionResult detectPoses(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException(
            String.format("Pose detection is not supported by backend '%s'", getBackendId()));
    }

    /**
     * Performs hand detection on the provided image data.
     *
     * @param imageData the image data to process
     * @return the hand detection results
     * @throws BaseVisionException if hand detection fails
     * @throws IllegalArgumentException if imageData is null or invalid
     */
    default VisionResult detectHands(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException(
            String.format("Hand detection is not supported by backend '%s'", getBackendId()));
    }

    /**
     * Performs custom detection on the provided image data.
     *
     * @param imageData the image data to process
     * @return the custom detection results
     * @throws BaseVisionException if custom detection fails
     * @throws IllegalArgumentException if imageData is null or invalid
     */
    default VisionResult detectCustom(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException(
            String.format("Custom detection is not supported by backend '%s'", getBackendId()));
    }

    /**
     * Performs multiple detection types on the provided image data.
     *
     * <p>This method allows performing multiple detection operations in a single
     * call, which can be more efficient than multiple individual calls.</p>
     *
     * @param imageData the image data to process
     * @param detectionTypes the types of detection to perform
     * @return a list of detection results in the same order as the detection types
     * @throws BaseVisionException if any detection fails
     * @throws IllegalArgumentException if imageData is null, detectionTypes is null/empty, or any detection type is not supported
     */
    default List<VisionResult> detectMultiple(ImageData imageData, List<DetectionType> detectionTypes)
            throws BaseVisionException {
        if (imageData == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }
        if (detectionTypes == null || detectionTypes.isEmpty()) {
            throw new IllegalArgumentException("Detection types must not be null or empty");
        }

        // Validate all detection types are supported
        for (DetectionType detectionType : detectionTypes) {
            if (!supportsDetectionType(detectionType)) {
                throw new IllegalArgumentException(
                    String.format("Detection type '%s' is not supported by backend '%s'",
                        detectionType.getDisplayName(), getBackendId()));
            }
        }

        return detectionTypes.stream()
            .map(detectionType -> {
                try {
                    return detect(imageData, detectionType);
                } catch (BaseVisionException e) {
                    throw new RuntimeException(e);
                }
            })
            .toList();
    }

    /**
     * Initializes the backend and performs any necessary setup.
     *
     * <p>This method should be called before using the backend to ensure
     * it's properly initialized. It may load models, establish connections,
     * or perform other initialization tasks.</p>
     *
     * @throws BaseVisionException if initialization fails
     */
    default void initialize() throws BaseVisionException {
        // Default implementation does nothing
    }

    /**
     * Shuts down the backend and releases any resources.
     *
     * <p>This method should be called when the backend is no longer needed
     * to properly clean up resources, close connections, etc.</p>
     *
     * @throws BaseVisionException if shutdown fails
     */
    default void shutdown() throws BaseVisionException {
        // Default implementation does nothing
    }
}
