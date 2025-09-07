package com.springvision.jpa.dto;

import java.util.Map;

/**
 * Request DTO for storing a face embedding.
 *
 * Records are used to keep the DTO immutable and compact.
 */
public record StoreFaceEmbeddingRequest(
    String personId,
    float[] embedding,
    String modelName,
    String imageHash,
    Double confidence,
    Map<String, Object> metadata
) {} 