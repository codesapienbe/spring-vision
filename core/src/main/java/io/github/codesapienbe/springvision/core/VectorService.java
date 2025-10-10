package io.github.codesapienbe.springvision.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight abstraction for vector similarity operations used by VisionTemplate.
 * Implementations may live in optional modules (JPA, PGVector, etc.).
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
public interface VectorService {

    /**
     * Store a face embedding and return the stored embedding id.
     *
     * @param personId   the ID of the person associated with the embedding
     * @param embedding  the face embedding vector
     * @param modelName  the name of the model that generated the embedding
     * @param imageHash  the hash of the image from which the embedding was extracted
     * @param confidence the confidence score of the detection
     * @param metadata   additional metadata to store with the embedding
     * @return the unique ID of the stored embedding
     */
    String storeFaceEmbedding(String personId,
                              float[] embedding,
                              String modelName,
                              String imageHash,
                              Double confidence,
                              Map<String, Object> metadata);

    /**
     * Find similar face embeddings.
     *
     * @param queryEmbedding   the embedding to find similarities for
     * @param modelName        the name of the model to use for comparison
     * @param metric           the distance metric to use (e.g., "cosine", "euclidean")
     * @param threshold        the similarity threshold
     * @param limit            the maximum number of results to return
     * @param includePersonIds an optional set of person IDs to include in the search
     * @param excludePersonIds an optional set of person IDs to exclude from the search
     * @return a list of maps, where each map represents a similar face and contains keys
     *         such as "embeddingId", "personId", "similarity", "distance", "modelName", and "createdAt"
     */
    List<Map<String, Object>> findSimilarFaces(float[] queryEmbedding,
                                               String modelName,
                                               String metric,
                                               Double threshold,
                                               Integer limit,
                                               Set<String> includePersonIds,
                                               Set<String> excludePersonIds);
}
