package io.github.codesapienbe.springvision.health.api;

/**
 * Custom checked exception for errors that occur within the health module.
 * This exception can be used to signal issues related to health monitoring tasks,
 * such as fall detection, stress analysis, or brain tumor classification.
 */
public class HealthException extends Exception {

    /**
     * Constructs a new HealthException with the specified detail message.
     *
     * @param message the detail message.
     */
    public HealthException(String message) {
        super(message);
    }

    /**
     * Constructs a new HealthException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause the cause of the exception.
     */
    public HealthException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new HealthException with the specified cause.
     *
     * @param cause the cause of the exception.
     */
    public HealthException(Throwable cause) {
        super(cause);
    }
}
