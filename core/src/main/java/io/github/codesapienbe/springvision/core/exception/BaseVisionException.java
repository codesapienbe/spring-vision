package io.github.codesapienbe.springvision.core.exception;

/**
 * Base exception class for all Spring Vision operations.
 *
 * <p>This exception serves as the foundation for all vision-related exceptions
 * in the Spring Vision framework. It provides a common base for handling errors
 * that occur during computer vision operations such as image processing,
 * detection failures, backend communication issues, etc.</p>
 *
 * <p>The exception includes contextual information about the operation that
 * failed, making it easier to diagnose and handle errors appropriately.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try {
 *     VisionResult result = visionTemplate.detectFaces(imageData);
 * } catch (BaseVisionException e) {
 *     logger.error("Vision operation failed: {}", e.getMessage(), e);
 *     // Handle the error appropriately
 * }
 * }</pre>
 *
 * @author Spring Vision Team
 * @see VisionProcessingException
 * @see VisionBackendException
 * @see VisionConfigurationException
 * @since 1.0.0
 */
public abstract class BaseVisionException extends RuntimeException {

    /**
     * The operation that was being performed when the exception occurred.
     */
    private final String operation;

    /**
     * The type of detection that was being performed.
     */
    private final String detectionType;

    /**
     * Additional context information about the error.
     */
    private final String context;

    /**
     * Constructs a new BaseVisionException with the specified message.
     *
     * @param message the detail message describing the error
     */
    protected BaseVisionException(String message) {
        super(message);
        this.operation = null;
        this.detectionType = null;
        this.context = null;
    }

    /**
     * Constructs a new BaseVisionException with the specified message and cause.
     *
     * @param message the detail message describing the error
     * @param cause   the cause of the exception
     */
    protected BaseVisionException(String message, Throwable cause) {
        super(message, cause);
        this.operation = null;
        this.detectionType = null;
        this.context = null;
    }

    /**
     * Constructs a new BaseVisionException with operation context.
     *
     * @param message       the detail message describing the error
     * @param operation     the operation that was being performed
     * @param detectionType the type of detection being performed
     */
    protected BaseVisionException(String message, String operation, String detectionType) {
        super(message);
        this.operation = operation;
        this.detectionType = detectionType;
        this.context = null;
    }

    /**
     * Constructs a new BaseVisionException with operation context and cause.
     *
     * @param message       the detail message describing the error
     * @param operation     the operation that was being performed
     * @param detectionType the type of detection being performed
     * @param cause         the cause of the exception
     */
    protected BaseVisionException(String message, String operation, String detectionType, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.detectionType = detectionType;
        this.context = null;
    }

    /**
     * Constructs a new BaseVisionException with full context.
     *
     * @param message       the detail message describing the error
     * @param operation     the operation that was being performed
     * @param detectionType the type of detection being performed
     * @param context       additional context information
     * @param cause         the cause of the exception
     */
    protected BaseVisionException(String message, String operation, String detectionType,
                                  String context, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.detectionType = detectionType;
        this.context = context;
    }

    /**
     * Gets the operation that was being performed when the exception occurred.
     *
     * @return the operation name, or null if not specified
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Gets the type of detection that was being performed.
     *
     * @return the detection type, or null if not specified
     */
    public String getDetectionType() {
        return detectionType;
    }

    /**
     * Gets additional context information about the error.
     *
     * @return the context information, or null if not specified
     */
    public String getContext() {
        return context;
    }

    /**
     * Checks if this exception has operation context.
     *
     * @return true if operation context is available, false otherwise
     */
    public boolean hasOperationContext() {
        return operation != null;
    }

    /**
     * Checks if this exception has detection type context.
     *
     * @return true if detection type context is available, false otherwise
     */
    public boolean hasDetectionTypeContext() {
        return detectionType != null;
    }

    /**
     * Checks if this exception has additional context information.
     *
     * @return true if additional context is available, false otherwise
     */
    public boolean hasContext() {
        return context != null;
    }

    /**
     * Gets a detailed error message including all available context.
     *
     * @return a detailed error message with context information
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());

        if (hasOperationContext()) {
            sb.append(" [Operation: ").append(operation).append("]");
        }

        if (hasDetectionTypeContext()) {
            sb.append(" [Detection Type: ").append(detectionType).append("]");
        }

        if (hasContext()) {
            sb.append(" [Context: ").append(context).append("]");
        }

        return sb.toString();
    }

    /**
     * Checks if this exception is retryable.
     *
     * @return true if the operation can be retried, false otherwise
     */
    public boolean isRetryable() {
        // Default implementation - subclasses can override
        return false;
    }

    @Override
    public String toString() {
        String s = getClass().getName();
        String message = getDetailedMessage();
        return (message != null) ? (s + ": " + message) : s;
    }
}
