package com.springvision.core.exception;

/**
 * Exception thrown when errors occur with vision configuration.
 *
 * <p>This exception is used to indicate failures that occur during configuration
 * operations such as loading configuration files, validating settings, or
 * setting up vision parameters.</p>
 *
 * <p>Examples of when this exception might be thrown:</p>
 * <ul>
 *   <li>Invalid configuration parameters</li>
 *   <li>Missing required configuration files</li>
 *   <li>Configuration file format errors</li>
 *   <li>Unsupported configuration options</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see BaseVisionException
 */
public class VisionConfigurationException extends BaseVisionException {

    /**
     * Constructs a new VisionConfigurationException with the specified message.
     *
     * @param message the detail message describing the configuration error
     */
    public VisionConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a new VisionConfigurationException with the specified message and cause.
     *
     * @param message the detail message describing the configuration error
     * @param cause the cause of the exception
     */
    public VisionConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new VisionConfigurationException with operation context.
     *
     * @param message the detail message describing the configuration error
     * @param operation the operation that was being performed
     * @param detectionType the type of detection being performed
     */
    public VisionConfigurationException(String message, String operation, String detectionType) {
        super(message, operation, detectionType);
    }

    /**
     * Constructs a new VisionConfigurationException with operation context and cause.
     *
     * @param message the detail message describing the configuration error
     * @param operation the operation that was being performed
     * @param detectionType the type of detection being performed
     * @param cause the cause of the exception
     */
    public VisionConfigurationException(String message, String operation, String detectionType, Throwable cause) {
        super(message, operation, detectionType, cause);
    }

    /**
     * Constructs a new VisionConfigurationException with full context.
     *
     * @param message the detail message describing the configuration error
     * @param operation the operation that was being performed
     * @param detectionType the type of detection being performed
     * @param context additional context information
     * @param cause the cause of the exception
     */
    public VisionConfigurationException(String message, String operation, String detectionType,
                                        String context, Throwable cause) {
        super(message, operation, detectionType, context, cause);
    }
}
