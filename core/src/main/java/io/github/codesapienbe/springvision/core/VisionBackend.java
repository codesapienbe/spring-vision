package io.github.codesapienbe.springvision.core;

import java.util.Set;

import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Service Provider Interface for computer vision backends.
 *
 * <p>This interface defines the contract that all vision backends must implement
 * to integrate with the Spring Vision framework. It provides metadata about the
 * backend including its identifier, version, health status, and supported detection types.</p>
 *
 * <p>The interface is designed to hold only backend metadata and capabilities information.
 * Actual detection operations are performed through capability interfaces that backends
 * implement (e.g., {@link io.github.codesapienbe.springvision.core.capabilities.FaceDetectionCapability},
 * {@link io.github.codesapienbe.springvision.core.capabilities.ObjectDetectionCapability}, etc.).</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Get available backends
 * ServiceLoader<VisionBackend> backends = ServiceLoader.load(VisionBackend.class);
 *
 * // Use a backend via VisionTemplate
 * VisionBackend opencvBackend = // ... get OpenCV backend
 * VisionTemplate template = new VisionTemplate(opencvBackend);
 * VisionResult result = template.detectFaces(imageData);
 * }</pre>
 *
 * <p>Implementations should be registered using the Java Service Provider
 * mechanism by creating a file {@code META-INF/services/io.github.codesapienbe.springvision.core.VisionBackend}
 * containing the fully qualified class name of the implementation.</p>
 *
 * @author Spring Vision Team
 * @see VisionTemplate
 * @see ImageData
 * @see VisionResult
 * @see DetectionType
 * @since 1.0.0
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
     * Initializes the backend and performs any necessary setup.
     *
     * @throws BaseVisionException if initialization fails
     */
    default void initialize() throws BaseVisionException {
        // Default no-op
    }

    /**
     * Shuts down the backend and releases resources.
     *
     * @throws BaseVisionException if shutdown fails
     */
    default void shutdown() throws BaseVisionException {
        // Default no-op
    }


}
