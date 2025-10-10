package io.github.codesapienbe.springvision.persistence.enums;

/**
 * Enumerates the supported similarity and distance metrics for comparing vector embeddings.
 */
public enum SimilarityMetric {
    /**
     * Cosine similarity, which measures the cosine of the angle between two vectors.
     * A value of 1 means the vectors are identical, 0 means they are orthogonal, and -1 means they are diametrically opposed.
     * This is often converted to a distance metric (1 - similarity).
     */
    COSINE,
    /**
     * Euclidean distance (L2 distance), the straight-line distance between two points in Euclidean space.
     */
    EUCLIDEAN,
    /**
     * The dot product of two vectors. For normalized vectors, this is equivalent to cosine similarity.
     */
    DOT_PRODUCT,
    /**
     * Manhattan distance (L1 distance), the sum of the absolute differences of their Cartesian coordinates.
     */
    MANHATTAN
}
