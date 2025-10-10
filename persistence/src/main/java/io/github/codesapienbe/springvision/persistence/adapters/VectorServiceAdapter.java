package io.github.codesapienbe.springvision.persistence.adapters;

import io.github.codesapienbe.springvision.core.VectorService;
import io.github.codesapienbe.springvision.persistence.dto.SimilaritySearchResult;
import io.github.codesapienbe.springvision.persistence.dto.StoreFaceEmbeddingRequest;
import io.github.codesapienbe.springvision.persistence.service.VectorSimilarityService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter exposing module VectorSimilarityService as core VectorService.
 */
public class VectorServiceAdapter implements VectorService {

    private final VectorSimilarityService delegate;

    public VectorServiceAdapter(VectorSimilarityService delegate) {
        this.delegate = delegate;
    }

    @Override
    public String storeFaceEmbedding(String personId, float[] embedding, String modelName, String imageHash, Double confidence, Map<String, Object> metadata) {
        StoreFaceEmbeddingRequest req = new StoreFaceEmbeddingRequest(personId, embedding, modelName, imageHash, confidence, metadata == null ? Map.of() : metadata);
        return delegate.storeFaceEmbedding(req);
    }

    @Override
    public List<Map<String, Object>> findSimilarFaces(float[] queryEmbedding, String modelName, String metric, Double threshold, Integer limit, Set<String> includePersonIds, Set<String> excludePersonIds) {
        io.github.codesapienbe.springvision.persistence.dto.SimilaritySearchRequest req = new io.github.codesapienbe.springvision.persistence.dto.SimilaritySearchRequest(
            queryEmbedding,
            modelName,
            null,
            io.github.codesapienbe.springvision.persistence.enums.SimilarityMetric.valueOf(metric == null ? "COSINE" : metric.toUpperCase()),
            threshold,
            limit,
            includePersonIds,
            excludePersonIds
        );

        List<SimilaritySearchResult> results = delegate.findSimilarFaces(req);
        List<Map<String, Object>> out = new ArrayList<>();
        DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        for (SimilaritySearchResult r : results) {
            Map<String, Object> m = new HashMap<>();
            m.put("embeddingId", r.embeddingId());
            m.put("personId", r.personId());
            m.put("similarity", r.similarity());
            m.put("distance", r.distance());
            m.put("modelName", r.modelName());
            m.put("createdAt", r.createdAt() == null ? null : r.createdAt().format(iso));
            m.put("metadata", r.metadata());
            out.add(m);
        }
        return out;
    }
}
