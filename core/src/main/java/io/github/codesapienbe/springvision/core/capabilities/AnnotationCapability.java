package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.AnnotationRequest;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.function.Predicate;

/**
 * Capability interface for image obscuring and annotation operations.
 */
public interface AnnotationCapability {

    /**
     * Obscures detections matching the filter (e.g., blur faces).
     */
    ImageData obscure(ImageData imageData, Predicate<Detection> filter) throws BaseVisionException;

    /**
     * Performs generic annotation (e.g., draw boxes/labels) specified by request.
     */
    ImageData annotate(ImageData imageData, AnnotationRequest request) throws BaseVisionException;
}
