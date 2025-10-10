package io.github.codesapienbe.springvision.facebytes.exceptions;

/**
 * Base exception type for FaceBytes (Java rewrite of DeepFace) module.
 */
public class DeepFaceException extends RuntimeException {

    /**
     * Constructs a new DeepFaceException with the specified message.
     *
     * @param message the detail message
     */
    public DeepFaceException(String message) {
        super(message);
    }

    /**
     * Constructs a new DeepFaceException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public DeepFaceException(String message, Throwable cause) {
        super(message, cause);
    }
}
