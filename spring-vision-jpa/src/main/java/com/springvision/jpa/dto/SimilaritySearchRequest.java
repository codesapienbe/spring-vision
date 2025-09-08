package com.springvision.jpa.dto;

import com.springvision.jpa.enums.SimilarityMetric;

import java.util.Set;

/**
 * DTO representing a similarity search request.
 */
public record SimilaritySearchRequest(
    float[] queryEmbedding,
    String modelName,
    String imageHash,
    SimilarityMetric metric,
    Double threshold,
    Integer limit,
    Set<String> includePersonIds,
    Set<String> excludePersonIds
) {} 