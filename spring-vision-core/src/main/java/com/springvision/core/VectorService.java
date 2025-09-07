package com.springvision.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight abstraction for vector similarity operations used by VisionTemplate.
 * Implementations may live in optional modules (JPA, PGVector, etc.).
 */
public interface VectorService {

    /**
     * Store a face embedding and return the stored embedding id.
     */
    String storeFaceEmbedding(String personId,
                              float[] embedding,
                              String modelName,
                              String imageHash,
                              Double confidence,
                              Map<String, Object> metadata);

    /**
     * Find similar face embeddings. Returns list of maps with at least keys:
     * "embeddingId", "personId", "similarity", "distance", "modelName", "createdAt".
     */
    List<Map<String, Object>> findSimilarFaces(float[] queryEmbedding,
                                               String modelName,
                                               String metric,
                                               Double threshold,
                                               Integer limit,
                                               Set<String> includePersonIds,
                                               Set<String> excludePersonIds);
} 