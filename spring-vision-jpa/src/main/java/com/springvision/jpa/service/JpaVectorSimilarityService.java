package com.springvision.jpa.service;

import com.springvision.jpa.dto.SimilaritySearchRequest;
import com.springvision.jpa.dto.SimilaritySearchResult;
import com.springvision.jpa.dto.StoreFaceEmbeddingRequest;
import com.springvision.jpa.entity.FaceEmbedding;
import com.springvision.jpa.repository.FaceEmbeddingRepository;
import com.springvision.jpa.util.VectorUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simple JPA-based vector similarity service. This is a fallback implementation
 * that stores embeddings as blobs and computes similarity in-memory.
 */
@Service
public class JpaVectorSimilarityService implements VectorSimilarityService {

    private final FaceEmbeddingRepository embeddingRepository;

    public JpaVectorSimilarityService(FaceEmbeddingRepository embeddingRepository) {
        this.embeddingRepository = embeddingRepository;
    }

    @Override
    public String storeFaceEmbedding(StoreFaceEmbeddingRequest request) {
        FaceEmbedding embedding = new FaceEmbedding();
        embedding.setPersonId(request.personId());
        embedding.setModelName(request.modelName());
        embedding.setDimension(request.embedding() == null ? 0 : request.embedding().length);
        embedding.setEmbeddingBlob(VectorUtils.serializeFloatArray(request.embedding()));
        embedding.setImageHash(request.imageHash());
        embedding.setConfidence(request.confidence());

        FaceEmbedding saved = embeddingRepository.save(embedding);
        return saved.getId() == null ? null : saved.getId().toString();
    }

    @Override
    public List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request) {
        List<FaceEmbedding> allEmbeddings = embeddingRepository.findByModelName(request.modelName());

        return allEmbeddings.parallelStream()
            .map(embedding -> calculateSimilarity(embedding, request))
            .filter(result -> result.similarity() >= (request.threshold() == null ? 0.0 : request.threshold()))
            .sorted((a, b) -> Double.compare(b.similarity(), a.similarity()))
            .limit(request.limit() == null ? 100 : request.limit())
            .collect(Collectors.toList());
    }

    private SimilaritySearchResult calculateSimilarity(FaceEmbedding embedding, SimilaritySearchRequest request) {
        float[] storedVector = VectorUtils.deserializeFloatArray(embedding.getEmbeddingBlob());
        double similarity = 0.0;
        switch (request.metric()) {
            case COSINE -> similarity = VectorUtils.cosineSimilarity(request.queryEmbedding(), storedVector);
            case EUCLIDEAN -> {
                double dist = VectorUtils.euclideanDistance(request.queryEmbedding(), storedVector);
                // convert to similarity-like score (smaller distance -> larger similarity)
                similarity = 1.0 / (1.0 + dist);
            }
            default -> similarity = VectorUtils.cosineSimilarity(request.queryEmbedding(), storedVector);
        }

        return new SimilaritySearchResult(
            embedding.getId() == null ? null : embedding.getId().toString(),
            embedding.getPersonId(),
            similarity,
            1.0 - similarity,
            embedding.getModelName(),
            embedding.getCreatedAt(),
            Map.of()
        );
    }

    @Override
    public VerificationResult verifyFaces(VerificationRequest request) {
        double sim = VectorUtils.cosineSimilarity(request.getEmbeddingA(), request.getEmbeddingB());
        boolean match = sim >= 0.7; // default threshold; later configurable
        return new VerificationResult(match, 1.0 - sim, sim);
    }

    @Override
    public void deleteFaceEmbedding(String embeddingId) {
        if (embeddingId == null) return;
        try {
            embeddingRepository.deleteById(java.util.UUID.fromString(embeddingId));
        } catch (Exception ignored) {
        }
    }

    @Override
    public VectorServiceHealth getHealth() {
        return new VectorServiceHealth(true, "ok");
    }

    @Override
    public Set<String> getSupportedMetrics() {
        return Set.of("COSINE", "EUCLIDEAN");
    }
} 