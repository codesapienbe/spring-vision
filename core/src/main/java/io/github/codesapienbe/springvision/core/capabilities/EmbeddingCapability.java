package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.DetectionCategory;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Capability interface for embedding extraction (e.g., face embeddings).
 */
public interface EmbeddingCapability {
    /**
     * Extracts embeddings from the provided image data for the specified subject category.
     *
     * @param imageData the image data to process
     * @param subject   the detection category (e.g., FACE, OBJECT)
     * @return a list of embedding vectors extracted from the image
     * @throws BaseVisionException if embedding extraction fails
     */
    List<float[]> extractEmbeddings(ImageData imageData, DetectionCategory subject) throws BaseVisionException;
}
