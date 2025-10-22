package io.github.codesapienbe.springvision.core.capabilities;

import java.util.Map;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Capability interface for image classification.
 *
 * <p>Backends implementing this interface can classify images into categories.</p>
 */
public interface ImageClassificationCapability {

    /**
     * Classifies the provided image.
     *
     * @param imageData The image to classify.
     * @param topK      Number of top predictions to return (default: 5)
     * @return Classification results with labels and confidence scores
     * @throws BaseVisionException if classification fails
     */
    ClassificationResult classifyImage(ImageData imageData, int topK) throws BaseVisionException;

    /**
     * Classifies the provided image (returns top 5 results).
     *
     * @param imageData The image to classify.
     * @return Classification results with labels and confidence scores
     * @throws BaseVisionException if classification fails
     */
    default ClassificationResult classifyImage(ImageData imageData) throws BaseVisionException {
        return classifyImage(imageData, 5);
    }

    /**
     * Represents the result of image classification.
     */
    record ClassificationResult(
        java.util.List<Classification> classifications,
        Map<String, Object> metadata
    ) {
    }

    /**
     * Represents a single classification with label and confidence.
     */
    record Classification(
        String label,
        double confidence,
        Map<String, Object> attributes
    ) {
    }

    /**
     * Checks if image classification models are available and loaded.
     *
     * @return true if image classification models are available, false otherwise
     */
    boolean isImageClassificationModelAvailable();
}
