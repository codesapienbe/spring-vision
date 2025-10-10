package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.DetectionCategory;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;

/**
 * Capability interface for embedding extraction (e.g., face embeddings).
 */
public interface EmbeddingCapability {
    List<float[]> extractEmbeddings(ImageData imageData, DetectionCategory subject) throws BaseVisionException;
}
