package io.github.codesapienbe.springvision.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines the contract for storing and querying vector embeddings, typically used for
 * face recognition and other similarity search tasks.
 * <p>
 * This lightweight abstraction allows {@link VisionTemplate} to perform vector-based
 * operations without being tied to a specific storage implementation. Implementations
 * can range from in-memory stores to dedicated vector databases like PGVector or JPA-based
 * solutions, and are expected to be provided in optional modules.
 *
 * @author Spring Vision Team
 * @see VisionTemplate
 * @since 1.0.0
 */
public interface VectorService {

    /**
     * Stores a face embedding in the vector database and returns a unique identifier for it.
     *
     * @param personId   The identifier for the person associated with the face embedding.
     * @param embedding  The float array representing the face embedding vector.
     * @param modelName  The name of the model that generated the embedding.
     * @param imageHash  A hash of the source image, used for deduplication.
     * @param confidence The confidence score of the face detection.
     * @param metadata   A map of additional metadata to store alongside the embedding.
     * @return The unique ID assigned to the stored embedding.
     */
    String storeFaceEmbedding(String personId,
                              float[] embedding,
                              String modelName,
                              String imageHash,
                              Double confidence,
                              Map<String, Object> metadata);

    /**
     * Searches for face embeddings that are similar to a given query embedding.
     *
     * @param queryEmbedding   The embedding to find similarities for.
     * @param modelName        The name of the model to use for comparison, ensuring compatibility.
     * @param metric           The distance metric to use (e.g., "cosine", "euclidean").
     * @param threshold        The similarity threshold for including a result.
     * @param limit            The maximum number of similar faces to return.
     * @param includePersonIds An optional set of person IDs to restrict the search to.
     * @param excludePersonIds An optional set of person IDs to exclude from the search.
     * @return A list of maps, where each map represents a similar face and contains details
     * like "embeddingId", "personId", "similarity", "distance", "modelName", and "createdAt".
     */
    List<Map<String, Object>> findSimilarFaces(float[] queryEmbedding,
                                               String modelName,
                                               String metric,
                                               Double threshold,
                                               Integer limit,
                                               Set<String> includePersonIds,
                                               Set<String> excludePersonIds);

    /**
     * Find stored entries that match the given image hash.
     * Returns a list of maps containing at least keys: "embeddingId", "personId", "modelName", "createdAt".
     */
    List<Map<String, Object>> findEntriesByImageHash(String imageHash);

    /**
     * Delete a stored embedding by its id.
     */
    void deleteEmbeddingById(String embeddingId);

    /**
     * Find stored entries for a given person id.
     * Returns a list of maps containing at least keys: "embeddingId", "personId", "modelName", "createdAt".
     */
    List<Map<String, Object>> findEntriesByPersonId(String personId);

    // New: utility methods for similarity and embedding serialization

    /**
     * Calculate cosine similarity between two embeddings. Default implementation delegates to VectorUtils.
     */
    default double cosineSimilarity(float[] a, float[] b) {
        return VectorUtils.cosineSimilarity(a, b);
    }

    /**
     * Calculate euclidean distance between two embeddings. Default implementation delegates to VectorUtils.
     */
    default double euclideanDistance(float[] a, float[] b) {
        return VectorUtils.euclideanDistance(a, b);
    }

    /**
     * Calculate manhattan distance between two embeddings. Default implementation delegates to VectorUtils.
     */
    default double manhattanDistance(float[] a, float[] b) {
        return VectorUtils.manhattanDistance(a, b);
    }

    /**
     * Convert float[] embedding to a byte[] representation (big-endian floats).
     */
    default byte[] embeddingToBytes(float[] arr) {
        return VectorUtils.embeddingToBytes(arr);
    }

    /**
     * Compute a set of similarity metrics and a combined score for two embeddings.
     * Returns a map with keys: cosineSimilarity, euclideanDistance, manhattanDistance,
     * euclideanSimilarity, manhattanSimilarity, combinedSimilarity
     */
    default Map<String, Object> computeSimilarityMetrics(float[] a, float[] b) {
        return VectorUtils.computeSimilarityMetrics(a, b);
    }
}
