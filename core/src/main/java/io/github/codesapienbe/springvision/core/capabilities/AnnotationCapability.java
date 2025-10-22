package io.github.codesapienbe.springvision.core.capabilities;

import java.util.function.Predicate;

import io.github.codesapienbe.springvision.core.AnnotationRequest;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Capability interface for image obscuring and annotation operations.
 */
public interface AnnotationCapability {

    /**
     * Obscures detections matching the filter (e.g., blur faces).
     * @param imageData The image to obscure.
     * @param filter A predicate to select which detections to obscure.
     * @return A new image with the selected detections obscured.
     * @throws BaseVisionException if an error occurs during the operation.
     */
    ImageData obscure(ImageData imageData, Predicate<Detection> filter) throws BaseVisionException;

    /**
     * Performs generic annotation (e.g., draw boxes/labels) specified by request.
     * @param imageData The image to annotate.
     * @param request The annotation request.
     * @return A new image with the requested annotations.
     * @throws BaseVisionException if an error occurs during the operation.
     */
    ImageData annotate(ImageData imageData, AnnotationRequest request) throws BaseVisionException;

    /**
     * Checks if annotation capabilities are available and loaded.
     *
     * @return true if annotation capabilities are available, false otherwise
     */
    boolean isAnnotationModelAvailable();
}
