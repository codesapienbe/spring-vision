package com.springvision.core.exception;

/**
 * Exception thrown when a requested vision capability or detection type is not supported
 * by a particular backend implementation.
 *
 * <p>This is a runtime exception that extends {@link BaseVisionException} so callers
 * can catch vision-specific errors centrally. Constructors allow providing operation
 * and detection-type context to aid diagnostics and monitoring.</p>
 */
public class VisionUnsupportedException extends BaseVisionException {

    /**
     * Constructs a new VisionUnsupportedException with the specified message.
     *
     * @param message detailed error message
     */
    public VisionUnsupportedException(String message) {
        super(message);
    }

    /**
     * Constructs a new VisionUnsupportedException with operation and detection context.
     *
     * @param message detailed error message
     * @param operation the operation being performed (e.g., "detectText")
     * @param detectionType the detection type or null
     */
    public VisionUnsupportedException(String message, String operation, String detectionType) {
        super(message, operation, detectionType);
    }

    /**
     * Constructs a new VisionUnsupportedException with message and cause.
     *
     * @param message detailed error message
     * @param cause the root cause
     */
    public VisionUnsupportedException(String message, Throwable cause) {
        super(message, cause);
    }
} 