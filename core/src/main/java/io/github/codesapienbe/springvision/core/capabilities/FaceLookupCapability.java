package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;

/**
 * Capability for looking up nearest embeddings in a gallery (face search / nearest-neighbor).
 */
public interface FaceLookupCapability {

    /**
     * Finds the nearest embeddings in a gallery to a given probe embedding.
     * Implementations must provide their own nearest-neighbor search logic.
     *
     * @param probeImage        The probe image data.
     * @param probeEmbedding    The embedding of the probe face.
     * @param galleryEmbeddings A list of embeddings to search through.
     * @param metric            The distance metric to use (e.g., "cosine" or "euclidean").
     * @param topK              The number of nearest neighbors to return.
     * @return A list of indices of the nearest embeddings in the gallery.
     * @throws BaseVisionException if an error occurs during the search.
     */
    List<Integer> findNearestEmbeddings(ImageData probeImage, float[] probeEmbedding, List<float[]> galleryEmbeddings, String metric, int topK) throws BaseVisionException;

}
