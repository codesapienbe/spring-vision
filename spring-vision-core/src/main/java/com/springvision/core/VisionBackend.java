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
     * <p>Default implementation routes via capability interfaces when available.
     * Implementations may override for backend-specific routing.</p>
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

        switch (detectionType) {
            case FACE -> {
                if (this instanceof com.springvision.core.capabilities.FaceDetectionCapability cap) {
                    return cap.detectFaces(imageData);
                }
                throw new UnsupportedOperationException("FACE detection not supported by capabilities of backend '" + getBackendId() + "'");
            }
            case OBJECT -> {
                if (this instanceof com.springvision.core.capabilities.ObjectDetectionCapability cap) {
                    return cap.detectObjects(imageData);
                }
                throw new UnsupportedOperationException("OBJECT detection not supported by capabilities of backend '" + getBackendId() + "'");
            }
            case TEXT -> {
                if (this instanceof com.springvision.core.capabilities.TextOcrCapability cap) {
                    return cap.detectText(imageData);
                }
                return detectText(imageData);
            }
            case BARCODE -> {
                if (this instanceof com.springvision.core.capabilities.BarcodeCapability cap) {
                    return cap.detectBarcodes(imageData);
                }
                return detectBarcodes(imageData);
            }
            case LANDMARK -> {
                if (this instanceof com.springvision.core.capabilities.LandmarkDetectionCapability cap) {
                    return cap.detectLandmarks(imageData);
                }
                return detectLandmarks(imageData);
            }
            case POSE -> {
                if (this instanceof com.springvision.core.capabilities.PoseEstimationCapability cap) {
                    return cap.detectPoses(imageData);
                }
                return detectPoses(imageData);
            }
            case HAND -> {
                if (this instanceof com.springvision.core.capabilities.HandDetectionCapability cap) {
                    return cap.detectHands(imageData);
                }
                return detectHands(imageData);
            }
            case CUSTOM -> {
                return detectCustom(imageData);
            }
            default -> throw new UnsupportedOperationException("Unsupported detection type: " + detectionType);
        }
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
     * Default implementation delegates to FaceBytes support if available.
     */
    default java.util.List<float[]> extractEmbeddings(ImageData imageData) throws BaseVisionException {
        return com.springvision.core.util.EmbeddingSupport.defaultExtractEmbeddings(imageData);
    }

    /**
     * Verifies whether two images belong to the same identity using embeddings.
     */
    default boolean verify(ImageData a, ImageData b, String metric, double threshold) throws BaseVisionException {
        return com.springvision.core.util.EmbeddingSupport.defaultVerify(a, b, metric, threshold);
    }
}
