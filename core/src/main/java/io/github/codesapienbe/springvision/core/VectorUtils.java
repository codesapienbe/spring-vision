package io.github.codesapienbe.springvision.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility routines for vector/embedding math shared across the project.
 * Kept package-private (core package) so other modules can reuse via VectorService defaults
 * or by referencing VectorUtils directly when no VectorService is configured.
 */
public final class VectorUtils {

    private VectorUtils() {
    }

    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length)
            throw new IllegalArgumentException("Embeddings must have the same length");
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na <= 0.0 || nb <= 0.0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    public static double euclideanDistance(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length)
            throw new IllegalArgumentException("Embeddings must have the same length");
        double s = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            s += d * d;
        }
        return Math.sqrt(s);
    }

    public static double manhattanDistance(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length)
            throw new IllegalArgumentException("Embeddings must have the same length");
        double s = 0.0;
        for (int i = 0; i < a.length; i++) s += Math.abs(a[i] - b[i]);
        return s;
    }

    public static byte[] embeddingToBytes(float[] arr) {
        if (arr == null) return new byte[0];
        byte[] out = new byte[arr.length * 4];
        for (int i = 0; i < arr.length; i++) {
            int bits = Float.floatToIntBits(arr[i]);
            out[i * 4] = (byte) ((bits >> 24) & 0xFF);
            out[i * 4 + 1] = (byte) ((bits >> 16) & 0xFF);
            out[i * 4 + 2] = (byte) ((bits >> 8) & 0xFF);
            out[i * 4 + 3] = (byte) (bits & 0xFF);
        }
        return out;
    }

    public static Map<String, Object> computeSimilarityMetrics(float[] a, float[] b) {
        Map<String, Object> m = new HashMap<>();
        double cosine = cosineSimilarity(a, b);
        double euclid = euclideanDistance(a, b);
        double manhattan = manhattanDistance(a, b);
        double euclidSim = 1.0 / (1.0 + euclid);
        double manhattanSim = 1.0 / (1.0 + manhattan / (a == null ? 1 : a.length));
        double combined = (cosine * 0.5) + (euclidSim * 0.3) + (manhattanSim * 0.2);

        m.put("cosineSimilarity", cosine);
        m.put("euclideanDistance", euclid);
        m.put("manhattanDistance", manhattan);
        m.put("euclideanSimilarity", euclidSim);
        m.put("manhattanSimilarity", manhattanSim);
        m.put("combinedSimilarity", combined);
        return m;
    }
}

