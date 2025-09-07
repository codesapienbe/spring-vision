package com.springvision.jpa.service;

import com.springvision.jpa.dto.SimilaritySearchRequest;
import com.springvision.jpa.dto.SimilaritySearchResult;
import com.springvision.jpa.repository.PostgreSQLFaceEmbeddingRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PostgreSQL-backed vector service using pgvector extension.
 */
@Service
@ConditionalOnProperty(value = "spring.vision.vector.provider", havingValue = "pgvector")
@ConditionalOnClass(name = "org.postgresql.util.PGobject")
public class PostgreSQLVectorSimilarityService implements VectorSimilarityService {

    private final PostgreSQLFaceEmbeddingRepository repository;

    public PostgreSQLVectorSimilarityService(PostgreSQLFaceEmbeddingRepository repository) {
        this.repository = repository;
    }

    @Override
    public String storeFaceEmbedding(com.springvision.jpa.dto.StoreFaceEmbeddingRequest request) {
        throw new UnsupportedOperationException("PostgreSQL service write not implemented yet");
    }

    @Override
    public List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request) {
        String vectorString = formatPgVector(request.queryEmbedding());

        List<Object[]> results = repository.findSimilarByCosineSimilarity(vectorString, request.modelName(), request.threshold(), request.limit());

        return results.stream()
            .map(this::mapToSimilarityResult)
            .collect(Collectors.toList());
    }

    private SimilaritySearchResult mapToSimilarityResult(Object[] row) {
        // Expecting: id, person_id, model_name, created_at, confidence, distance
        String id = row[0] == null ? null : row[0].toString();
        String personId = row[1] == null ? null : row[1].toString();
        String modelName = row[2] == null ? null : row[2].toString();
        java.time.LocalDateTime createdAt = row[3] == null ? null : (java.time.LocalDateTime) row[3];
        Double confidence = row[4] == null ? null : ((Number) row[4]).doubleValue();
        Double distance = row[5] == null ? null : ((Number) row[5]).doubleValue();

        double similarity = distance == null ? 0.0 : 1.0 - distance;

        return new SimilaritySearchResult(id, personId, similarity, distance, modelName, createdAt, java.util.Map.of("confidence", confidence));
    }

    private String formatPgVector(float[] embedding) {
        if (embedding == null) return "[]";
        return "[" + Arrays.stream(embedding).mapToObj(String::valueOf).collect(Collectors.joining(",")) + "]";
    }

    @Override
    public VerificationResult verifyFaces(VerificationRequest request) {
        double sim = com.springvision.jpa.util.VectorUtils.cosineSimilarity(request.getEmbeddingA(), request.getEmbeddingB());
        boolean match = sim >= 0.7;
        return new VerificationResult(match, 1.0 - sim, sim);
    }

    @Override
    public void deleteFaceEmbedding(String embeddingId) {
        // Deletion via generic repository not implemented for PostgreSQL-specific repo
    }

    @Override
    public VectorServiceHealth getHealth() {
        return new VectorServiceHealth(true, "ok");
    }

    @Override
    public java.util.Set<String> getSupportedMetrics() {
        return java.util.Set.of("COSINE");
    }
} 