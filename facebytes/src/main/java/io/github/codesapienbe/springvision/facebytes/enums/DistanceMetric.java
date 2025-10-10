package io.github.codesapienbe.springvision.facebytes.enums;

/**
 * Enumerates the supported distance metrics for comparing face embedding vectors.
 * The choice of metric depends on the facial recognition model used and the desired properties of the comparison.
 */
public enum DistanceMetric {
    /**
     * Cosine similarity measures the cosine of the angle between two vectors.
     * It is a measure of orientation and not magnitude. It is one of the most common metrics for face verification.
     * A smaller cosine distance means higher similarity.
     */
    COSINE,
    /**
     * Euclidean distance is the straight-line distance between two points in Euclidean space.
     * It is sensitive to the magnitude of the embeddings.
     */
    EUCLIDEAN,
    /**
     * L2-normalized Euclidean distance. Before computing the Euclidean distance, the embedding vectors are normalized to have a unit length (L2 norm of 1).
     * This makes the distance metric less sensitive to variations in vector magnitude.
     */
    EUCLIDEAN_L2
}
