package io.github.codesapienbe.springvision.persistence.service;

import io.github.codesapienbe.springvision.persistence.dto.SimilaritySearchRequest;
import io.github.codesapienbe.springvision.persistence.dto.SimilaritySearchResult;
import io.github.codesapienbe.springvision.persistence.dto.StoreFaceEmbeddingRequest;

import java.util.List;
import java.util.Set;

/**
 * Defines the core service contract for storing, searching, and managing face embeddings in a vector database.
 */
public interface VectorSimilarityService {

    /**
     * Stores a new face embedding in the database.
     *
     * @param request The request object containing the embedding and associated metadata.
     * @return The unique identifier of the newly stored embedding record.
     */
    String storeFaceEmbedding(StoreFaceEmbeddingRequest request);

    /**
     * Finds faces in the database that are similar to a given query embedding.
     *
     * @param request The request object containing the query embedding and search parameters.
     * @return A list of {@link SimilaritySearchResult} objects, ordered by similarity.
     */
    List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request);

    /**
     * Verifies if two face embeddings represent the same person by comparing their distance against a threshold.
     *
     * @param request The request containing the two embeddings to compare.
     * @return A {@link VerificationResult} indicating whether the faces match.
     */
    VerificationResult verifyFaces(VerificationRequest request);

    /**
     * Deletes a face embedding from the database using its unique identifier.
     *
     * @param embeddingId The ID of the embedding to delete.
     */
    void deleteFaceEmbedding(String embeddingId);

    /**
     * Gets the current health status of the vector similarity service.
     *
     * @return A {@link VectorServiceHealth} object describing the service status.
     */
    VectorServiceHealth getHealth();

    /**
     * Gets the set of similarity metric names supported by this service (e.g., "COSINE", "EUCLIDEAN").
     *
     * @return A set of supported metric names.
     */
    Set<String> getSupportedMetrics();

    /**
     * Represents the result of a face verification operation.
     */
    public static final class VerificationResult {
        private final boolean match;
        private final double distance;
        private final double similarity;

        public VerificationResult(boolean match, double distance, double similarity) {
            this.match = match;
            this.distance = distance;
            this.similarity = similarity;
        }

        public boolean isMatch() {
            return match;
        }

        public double getDistance() {
            return distance;
        }

        public double getSimilarity() {
            return similarity;
        }
    }

    /**
     * Represents the health status of the vector service.
     */
    public static final class VectorServiceHealth {
        private final boolean ok;
        private final String message;

        public VectorServiceHealth(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        public boolean isOk() {
            return ok;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Represents a request to verify two face embeddings.
     */
    public static final class VerificationRequest {
        private final float[] embeddingA;
        private final float[] embeddingB;

        public VerificationRequest(float[] embeddingA, float[] embeddingB) {
            this.embeddingA = embeddingA;
            this.embeddingB = embeddingB;
        }

        public float[] getEmbeddingA() {
            return embeddingA;
        }

        public float[] getEmbeddingB() {
            return embeddingB;
        }
    }
}
