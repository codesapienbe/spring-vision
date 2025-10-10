package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;

/**
 * Capability for looking up nearest embeddings in a gallery (face search / nearest-neighbor).
 * <p>
 * Default implementation delegates to core EmbeddingSupport utilities.
 */
public interface FaceLookupCapability {

    default List<Integer> findNearestEmbeddings(ImageData probeImage, float[] probeEmbedding, List<float[]> galleryEmbeddings, String metric, int topK) throws BaseVisionException {
        return io.github.codesapienbe.springvision.core.util.EmbeddingSupport.findNearest(probeImage, probeEmbedding, galleryEmbeddings, metric, topK);
    }
}

