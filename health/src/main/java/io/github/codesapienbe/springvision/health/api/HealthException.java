package io.github.codesapienbe.springvision.health.api;

/**
 * Checked exception for health module errors.
 */
public class HealthException extends Exception {
    public HealthException(String message) {
        super(message);
    }

    public HealthException(String message, Throwable cause) {
        super(message, cause);
    }

    public HealthException(Throwable cause) {
        super(cause);
    }
}

