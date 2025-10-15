package io.github.codesapienbe.springvision.facebytes.utils;

/**
 * A utility class providing static methods for calculating common distance metrics between numerical vectors.
 * These metrics are fundamental for comparing face embeddings in verification and recognition tasks.
 */
public final class DistanceMetrics {

    private DistanceMetrics() {
    }

    /**
     * Calculates the cosine distance between two vectors.
     * Cosine distance is defined as 1 minus the cosine similarity. It ranges from 0 (identical vectors) to 2 (opposite vectors).
     *
     * @param a The first vector.
     * @param b The second vector.
     * @return The cosine distance between the two vectors.
     * @throws IllegalArgumentException if the vectors are null or have different lengths.
     */
    public static double cosineDistance(double[] a, double[] b) {
        validateSameLength(a, b);
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        if (denom == 0.0) return 1.0;
        double cos = dot / denom;
        return 1.0 - Math.max(-1.0, Math.min(1.0, cos));
    }

    /**
     * Calculates the Euclidean distance (or L2 distance) between two vectors.
     * This is the straight-line distance between two points in Euclidean space.
     *
     * @param a The first vector.
     * @param b The second vector.
     * @return The Euclidean distance.
     * @throws IllegalArgumentException if the vectors are null or have different lengths.
     */
    public static double euclideanDistance(double[] a, double[] b) {
        validateSameLength(a, b);
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    /**
     * Calculates the L2-normalized Euclidean distance. In this context, it is equivalent to the standard Euclidean distance.
     * This method is often used for clarity in contexts where embeddings are assumed to be L2-normalized.
     *
     * @param a The first vector.
     * @param b The second vector.
     * @return The Euclidean distance.
     */
    public static double euclideanL2Distance(double[] a, double[] b) {
        return euclideanDistance(a, b);
    }

    private static void validateSameLength(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("Vectors must be non-null and have same length");
        }
    }
}
