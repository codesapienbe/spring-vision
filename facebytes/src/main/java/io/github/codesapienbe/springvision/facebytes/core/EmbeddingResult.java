package io.github.codesapienbe.springvision.facebytes.core;

/**
 * Represents the result of a face embedding operation.
 * This record pairs the generated numerical embedding with the specific face region from which it was extracted.
 *
 * @param embedding  The floating-point vector representing the face embedding. This vector is a compact, numerical representation of the face's features.
 * @param faceRegion The {@link FaceRegion} object that defines the location and detection details of the face from which the embedding was generated.
 */
public record EmbeddingResult(float[] embedding, FaceRegion faceRegion) {
}
