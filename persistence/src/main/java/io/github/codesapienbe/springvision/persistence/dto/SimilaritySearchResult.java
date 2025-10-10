package io.github.codesapienbe.springvision.persistence.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Result DTO for similarity search operations.
 *
 * @param embeddingId the unique identifier of the embedding
 * @param personId    the identifier of the person associated with the embedding
 * @param similarity  the similarity score (higher is more similar)
 * @param distance    the distance metric value (lower is more similar)
 * @param modelName   the name of the model used for the embedding
 * @param createdAt   the timestamp when the embedding was created
 * @param metadata    additional metadata associated with the result
 */
public record SimilaritySearchResult(
    String embeddingId,
    String personId,
    Double similarity,
    Double distance,
    String modelName,
    LocalDateTime createdAt,
    Map<String, Object> metadata
) {
}
