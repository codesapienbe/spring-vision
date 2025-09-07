package com.springvision.jpa.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Result DTO for similarity search operations.
 */
public record SimilaritySearchResult(
    String embeddingId,
    String personId,
    Double similarity,
    Double distance,
    String modelName,
    LocalDateTime createdAt,
    Map<String, Object> metadata
) {} 