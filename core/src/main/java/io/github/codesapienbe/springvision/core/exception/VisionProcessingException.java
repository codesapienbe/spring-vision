package io.github.codesapienbe.springvision.core.exception;

/**
 * Exception thrown when errors occur during vision processing operations.
 *
 * <p>This exception is used to indicate failures that occur during the actual
 * processing of images for computer vision tasks such as face detection,
 * object detection, text recognition, etc.</p>
 *
 * <p>Examples of when this exception might be thrown:</p>
 * <ul>
 *   <li>Image format is not supported</li>
 *   <li>Image data is corrupted or invalid</li>
 *   <li>Processing algorithm fails to execute</li>
 *   <li>Memory issues during processing</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @see BaseVisionException
 * @since 1.0.0
 */
public class VisionProcessingException extends BaseVisionException {

    /**
     * Constructs a new VisionProcessingException with the specified message.
     *
     * @param message the detail message describing the processing error
     */
    public VisionProcessingException(String message) {
        super(message);
    }

    /**
     * Constructs a new VisionProcessingException with the specified message and cause.
     *
     * @param message the detail message describing the processing error
     * @param cause   the cause of the exception
     */
    public VisionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new VisionProcessingException with operation context.
     *
     * @param message       the detail message describing the processing error
     * @param operation     the operation that was being performed
     * @param detectionType the type of detection being performed
     */
    public VisionProcessingException(String message, String operation, String detectionType) {
        super(message, operation, detectionType);
    }

    /**
     * Constructs a new VisionProcessingException with operation context and cause.
     *
     * @param message       the detail message describing the processing error
     * @param operation     the operation that was being performed
     * @param detectionType the type of detection being performed
     * @param cause         the cause of the exception
     */
    public VisionProcessingException(String message, String operation, String detectionType, Throwable cause) {
        super(message, operation, detectionType, cause);
    }

    /**
     * Constructs a new VisionProcessingException with full context.
     *
     * @param message       the detail message describing the processing error
     * @param operation     the operation that was being performed
     * @param detectionType the type of detection being performed
     * @param context       additional context information
     * @param cause         the cause of the exception
     */
    public VisionProcessingException(String message, String operation, String detectionType,
                                     String context, Throwable cause) {
        super(message, operation, detectionType, context, cause);
    }
}
