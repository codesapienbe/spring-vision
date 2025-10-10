package io.github.codesapienbe.springvision.persistence.service;

import io.github.codesapienbe.springvision.persistence.dto.SimilaritySearchRequest;
import io.github.codesapienbe.springvision.persistence.dto.SimilaritySearchResult;
import io.github.codesapienbe.springvision.persistence.dto.StoreFaceEmbeddingRequest;

import java.util.List;
import java.util.Set;

/**
 * Core service interface for storing and searching face embeddings.
 */
public interface VectorSimilarityService {

    String storeFaceEmbedding(StoreFaceEmbeddingRequest request);

    List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request);

    VerificationResult verifyFaces(VerificationRequest request);

    void deleteFaceEmbedding(String embeddingId);

    VectorServiceHealth getHealth();

    Set<String> getSupportedMetrics();

    // Placeholder records for now – these will be replaced by richer types later
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
