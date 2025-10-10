package io.github.codesapienbe.springvision.persistence.dto;

import java.util.Map;

/**
 * Request DTO for storing a face embedding.
 * <p>
 * Records are used to keep the DTO immutable and compact.
 *
 * @param personId   the unique identifier for the person
 * @param embedding  the face embedding vector
 * @param modelName  the name of the model used to generate the embedding
 * @param imageHash  the hash of the source image
 * @param confidence the confidence score of the detection
 * @param metadata   additional metadata associated with the embedding
 */
public record StoreFaceEmbeddingRequest(
    String personId,
    float[] embedding,
    String modelName,
    String imageHash,
    Double confidence,
    Map<String, Object> metadata
) {
}
