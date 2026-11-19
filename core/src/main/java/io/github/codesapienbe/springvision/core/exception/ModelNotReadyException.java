package io.github.codesapienbe.springvision.core.exception;

/**
 * A model the operation needs is still being downloaded, or its download failed and will be
 * retried. The message says which model and how far along it is. Callers must report this as a
 * failure: the operation has not run.
 */
public class ModelNotReadyException extends VisionBackendException {

    /**
     * @param message which model is not ready and how far its download is
     */
    public ModelNotReadyException(String message) {
        super(message, "model_downloading", null);
    }
}
