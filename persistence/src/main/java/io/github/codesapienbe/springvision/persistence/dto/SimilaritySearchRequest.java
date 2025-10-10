package io.github.codesapienbe.springvision.persistence.dto;

import io.github.codesapienbe.springvision.persistence.enums.SimilarityMetric;

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
) {
}
