package io.github.codesapienbe.springvision.core.recognition;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for high-performance face embedding indexing and similarity search.
 *
 * <p>This interface provides the foundation for large-scale face recognition systems
 * that need to search through millions of face embeddings efficiently. Implementations
 * should use approximate nearest neighbor (ANN) algorithms for optimal performance.</p>
 *
 * <p>The interface supports both synchronous and asynchronous operations, making it
 * suitable for real-time queries as well as batch processing scenarios.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
public interface FaceEmbeddingIndex {

    /**
     * Add a single face embedding to the index.
     *
     * @param photoId   unique identifier for the photo containing the face
     * @param embedding normalized face embedding vector (typically 512 dimensions)
     * @throws IllegalArgumentException if photoId is null/empty or embedding is invalid
     */
    void addEmbedding(String photoId, float[] embedding);

    /**
     * Add multiple face embeddings to the index in batch.
     * More efficient than individual calls for large datasets.
     *
     * @param embeddings list of photo ID and embedding pairs
     * @throws IllegalArgumentException if any embedding is invalid
     */
    void addEmbeddingsBatch(List<EmbeddingEntry> embeddings);

    /**
     * Remove an embedding from the index.
     *
     * @param photoId identifier of the embedding to remove
     */
    void removeEmbedding(String photoId);

    /**
     * Search for similar face embeddings.
     *
     * @param queryEmbedding normalized query embedding vector
     * @param topK           number of top similar results to return
     * @return list of search results ordered by similarity (best first)
     * @throws IllegalArgumentException if queryEmbedding is null/invalid or {@code topK <= 0}
     * @throws IllegalStateException    if index is not ready
     */
    List<SearchResult> search(float[] queryEmbedding, int topK);

    /**
     * Search for similar embeddings with distance threshold.
     *
     * @param queryEmbedding normalized query embedding vector
     * @param topK           number of top similar results to return
     * @param maxDistance    maximum distance threshold (results beyond this are excluded)
     * @return list of search results within distance threshold
     */
    List<SearchResult> search(float[] queryEmbedding, int topK, double maxDistance);

    /**
     * Asynchronous search operation for high-throughput scenarios.
     *
     * @param queryEmbedding normalized query embedding vector
     * @param topK           number of top similar results to return
     * @return CompletableFuture containing search results
     */
    CompletableFuture<List<SearchResult>> searchAsync(float[] queryEmbedding, int topK);

    /**
     * Save the built index to persistent storage.
     *
     * @param indexPath path where to save the index
     * @throws RuntimeException      if saving fails
     * @throws IllegalStateException if index is not ready
     */
    void saveIndex(Path indexPath);

    /**
     * Load a previously saved index from storage.
     *
     * @param indexPath path to the saved index
     * @throws RuntimeException         if loading fails
     * @throws IllegalArgumentException if indexPath is invalid
     */
    void loadIndex(Path indexPath);

    /**
     * Clear all embeddings from the index.
     */
    void clear();

    /**
     * Rebuild the index from existing embeddings.
     * Must be called after adding embeddings and before searching.
     */
    void rebuild();

    /**
     * Get the number of embeddings in the index.
     *
     * @return total number of indexed embeddings
     */
    long size();

    /**
     * Check if the index is ready for searching.
     *
     * @return true if index is ready for queries
     */
    boolean isReady();

    /**
     * Get index statistics for monitoring and optimization.
     *
     * @return statistics object with performance metrics
     */
    IndexStatistics getStatistics();

    /**
     * Close the index and release resources.
     */
    void close();

    /**
     * Represents a single embedding entry for batch operations.
     *
     * @param photoId   the unique identifier for the photo containing the face
     * @param embedding the normalized face embedding vector
     */
    record EmbeddingEntry(String photoId, float[] embedding) {
        public EmbeddingEntry {
            if (photoId == null || photoId.trim().isEmpty()) {
                throw new IllegalArgumentException("Photo ID must not be null or empty");
            }
            if (embedding == null || embedding.length == 0) {
                throw new IllegalArgumentException("Embedding must not be null or empty");
            }
        }
    }

    /**
     * Represents a search result with similarity information.
     *
     * @param photoId    the identifier of the matched photo
     * @param similarity the similarity score (0.0 to 1.0)
     * @param distance   the distance score
     */
    record SearchResult(String photoId, double similarity, double distance) implements Comparable<SearchResult> {

        public SearchResult {
            if (photoId == null) {
                throw new IllegalArgumentException("Photo ID must not be null");
            }
            if (distance < 0.0) {
                throw new IllegalArgumentException("Distance must be non-negative");
            }
            if (similarity < 0.0 || similarity > 1.0) {
                throw new IllegalArgumentException("Similarity must be between 0.0 and 1.0");
            }
        }

        /**
         * Compare results by similarity (higher similarity first).
         */
        @Override
        public int compareTo(SearchResult other) {
            return Double.compare(other.similarity, this.similarity);
        }

        /**
         * Create a SearchResult from cosine distance.
         *
         * @param photoId        photo identifier
         * @param cosineDistance cosine distance (0.0 = identical, 2.0 = opposite)
         * @return SearchResult with converted similarity score
         */
        public static SearchResult fromCosineDistance(String photoId, double cosineDistance) {
            double similarity = Math.max(0.0, 1.0 - cosineDistance / 2.0);
            return new SearchResult(photoId, similarity, cosineDistance);
        }

        /**
         * Create a SearchResult from Euclidean distance.
         * Assumes normalized embeddings where max distance is ~2.0.
         *
         * @param photoId           photo identifier
         * @param euclideanDistance Euclidean L2 distance
         * @return SearchResult with converted similarity score
         */
        public static SearchResult fromEuclideanDistance(String photoId, double euclideanDistance) {
            double similarity = Math.max(0.0, 1.0 - euclideanDistance / 2.0);
            return new SearchResult(photoId, similarity, euclideanDistance);
        }
    }

    /**
     * Index performance and usage statistics.
     *
     * @param totalEmbeddings        the total number of embeddings in the index
     * @param embeddingDimension     the dimension of the embedding vectors
     * @param memoryUsageBytes       the memory usage in bytes
     * @param averageQueryTimeMillis the average query time in milliseconds
     * @param totalQueries           the total number of queries performed
     * @param built                  whether the index has been built
     * @param ready                  whether the index is ready for queries
     */
    record IndexStatistics(
        long totalEmbeddings,
        int embeddingDimension,
        long memoryUsageBytes,
        double averageQueryTimeMillis,
        long totalQueries,
        boolean built,
        boolean ready
    ) {
    }
}
