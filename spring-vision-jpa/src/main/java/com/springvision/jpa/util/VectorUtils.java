package com.springvision.jpa.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Locale;

/**
 * Utility methods for working with vector embeddings.
 */
public final class VectorUtils {

    private VectorUtils() {
        // Utility class
    }

    /**
     * Serialize a float array into a byte array using big-endian order.
     *
     * @param array the float array to serialize
     * @return byte[] representation
     */
    public static byte[] serializeFloatArray(float[] array) {
        if (array == null) return new byte[0];
        ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES * array.length).order(ByteOrder.BIG_ENDIAN);
        for (float v : array) {
            buffer.putFloat(v);
        }
        return buffer.array();
    }

    /**
     * Deserialize a byte array back into a float array. Expects length % 4 == 0.
     *
     * @param bytes the byte array
     * @return float[] deserialized
     */
    public static float[] deserializeFloatArray(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return new float[0];
        if (bytes.length % Float.BYTES != 0) {
            throw new IllegalArgumentException("Byte array length is not a multiple of 4");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        float[] result = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.getFloat();
        }
        return result;
    }

    /**
     * Calculate cosine similarity between two vectors. Returns value in [-1,1].
     * If one of the vectors has zero norm, returns 0.0.
     *
     * @param vec1 first vector
     * @param vec2 second vector
     * @return cosine similarity
     */
    public static double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null) throw new IllegalArgumentException("Vectors must not be null");
        if (vec1.length != vec2.length) throw new IllegalArgumentException("Vectors must have same length");

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            double a = vec1[i];
            double b = vec2[i];
            dot += a * b;
            normA += a * a;
            normB += b * b;
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        if (denom == 0.0) return 0.0;
        return dot / denom;
    }

    /**
     * Calculate Euclidean distance between two vectors.
     *
     * @param vec1 first vector
     * @param vec2 second vector
     * @return Euclidean distance
     */
    public static double euclideanDistance(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null) throw new IllegalArgumentException("Vectors must not be null");
        if (vec1.length != vec2.length) throw new IllegalArgumentException("Vectors must have same length");

        double sum = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            double diff = vec1[i] - vec2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * Calculate SHA-256 hex digest of provided image bytes.
     *
     * @param imageData raw image bytes
     * @return hex encoded SHA-256 hash
     */
    public static String calculateImageHash(byte[] imageData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(imageData == null ? new byte[0] : imageData);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b));
        }
        return sb.toString();
    }
} 