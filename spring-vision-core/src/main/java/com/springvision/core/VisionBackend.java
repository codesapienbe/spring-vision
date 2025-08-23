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
 * checking backend capabilities, health status, and performing detections.</p>
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

    /** Performs face detection on the provided image data. */
    VisionResult detectFaces(ImageData imageData) throws BaseVisionException;

    /** Performs object detection on the provided image data. */
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

    /** Performs text recognition (OCR) on the provided image data. */
    default VisionResult detectText(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException(
            String.format("Text detection is not supported by backend '%s'", getBackendId()));
    }

    /** Performs barcode/QR code detection on the provided image data. */
    default VisionResult detectBarcodes(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException(
            String.format("Barcode detection is not supported by backend '%s'", getBackendId()));
    }

    /** Performs landmark detection on the provided image data. */
    default VisionResult detectLandmarks(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException(
            String.format("Landmark detection is not supported by backend '%s'", getBackendId()));
    }

    /** Performs pose estimation on the provided image data. */
    default VisionResult detectPoses(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException(
            String.format("Pose detection is not supported by backend '%s'", getBackendId()));
    }

    /** Performs hand detection on the provided image data. */
    default VisionResult detectHands(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException(
            String.format("Hand detection is not supported by backend '%s'", getBackendId()));
    }

    /** Performs custom detection on the provided image data. */
    default VisionResult detectCustom(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException(
            String.format("Custom detection is not supported by backend '%s'", getBackendId()));
    }

    /** Performs multiple detection types on the provided image data. */
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

    /** Initializes the backend and performs any necessary setup. */
    default void initialize() throws BaseVisionException {
        // Default no-op
    }

    /** Shuts down the backend and releases resources. */
    default void shutdown() {
        // Default no-op
    }

    /**
     * Extracts face embeddings from the provided image.
     *
     * <p>Default implementation uses FaceBytes (DeepFace Java) under the hood to produce
     * L2-normalized embeddings. Backends can override to provide native embeddings (e.g., SFace).</p>
     *
     * @param imageData image to process
     * @return list of embeddings (one per detected face); may be empty
     * @throws BaseVisionException if processing fails
     */
    default List<float[]> extractEmbeddings(ImageData imageData) throws BaseVisionException {
        return com.springvision.core.util.EmbeddingSupport.defaultExtractEmbeddings(imageData);
    }

    /**
     * Verifies whether two images belong to the same identity using embeddings.
     *
     * <p>Default implementation extracts embeddings and computes distance using the given metric.</p>
     *
     * @param a first image
     * @param b second image
     * @param metric "cosine" or "euclidean"
     * @param threshold distance threshold for a match
     * @return true if match, false otherwise
     * @throws BaseVisionException if processing fails
     */
    default boolean verify(ImageData a, ImageData b, String metric, double threshold) throws BaseVisionException {
        return com.springvision.core.util.EmbeddingSupport.defaultVerify(a, b, metric, threshold);
    }
}
