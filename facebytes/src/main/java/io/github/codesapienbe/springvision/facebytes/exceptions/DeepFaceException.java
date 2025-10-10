package io.github.codesapienbe.springvision.facebytes.exceptions;

/**
 * Base exception type for FaceBytes (Java rewrite of DeepFace) module.
 */
public class DeepFaceException extends RuntimeException {

    public DeepFaceException(String message) {
        super(message);
    }

    public DeepFaceException(String message, Throwable cause) {
        super(message, cause);
    }
}
