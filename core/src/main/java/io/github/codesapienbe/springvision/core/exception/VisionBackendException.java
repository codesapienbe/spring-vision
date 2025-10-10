package io.github.codesapienbe.springvision.core.exception;

/**
 * Exception thrown when errors occur with the vision backend.
 *
 * <p>This exception is used to indicate failures that occur during backend
 * operations such as initialization, shutdown, or communication with the
 * underlying computer vision library (e.g., OpenCV).</p>
 *
 * <p>Examples of when this exception might be thrown:</p>
 * <ul>
 *   <li>Backend initialization fails</li>
 *   <li>Required native libraries are not available</li>
 *   <li>Backend communication errors</li>
 *   <li>Resource allocation failures</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @see BaseVisionException
 * @since 1.0.0
 */
public class VisionBackendException extends BaseVisionException {

    /**
     * Constructs a new VisionBackendException with the specified message.
     *
     * @param message the detail message describing the backend error
     */
    public VisionBackendException(String message) {
        super(message);
    }

    /**
     * Constructs a new VisionBackendException with the specified message and cause.
     *
     * @param message the detail message describing the backend error
     * @param cause   the cause of the exception
     */
    public VisionBackendException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new VisionBackendException with operation context.
     *
     * @param message       the detail message describing the backend error
     * @param operation     the operation that was being performed
     * @param detectionType the type of detection being performed
     */
    public VisionBackendException(String message, String operation, String detectionType) {
        super(message, operation, detectionType);
    }

    /**
     * Constructs a new VisionBackendException with operation context and cause.
     *
     * @param message       the detail message describing the backend error
     * @param operation     the operation that was being performed
     * @param detectionType the type of detection being performed
     * @param cause         the cause of the exception
     */
    public VisionBackendException(String message, String operation, String detectionType, Throwable cause) {
        super(message, operation, detectionType, cause);
    }

    /**
     * Constructs a new VisionBackendException with full context.
     *
     * @param message       the detail message describing the backend error
     * @param operation     the operation that was being performed
     * @param detectionType the type of detection being performed
     * @param context       additional context information
     * @param cause         the cause of the exception
     */
    public VisionBackendException(String message, String operation, String detectionType,
                                  String context, Throwable cause) {
        super(message, operation, detectionType, context, cause);
    }
}
