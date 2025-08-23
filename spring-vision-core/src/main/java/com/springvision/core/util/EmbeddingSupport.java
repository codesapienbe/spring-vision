package com.springvision.core.util;

import com.springvision.core.ImageData;
import com.springvision.core.exception.BaseVisionException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility support for default embeddings and verification using FaceBytes.
 *
 * <p>This class provides default implementations used by VisionBackend when a backend
 * does not override embedding/verification. It relies on the FaceBytes module if present.</p>
 */
public final class EmbeddingSupport {

    private EmbeddingSupport() {}

    public static List<float[]> defaultExtractEmbeddings(ImageData imageData) throws BaseVisionException {
        if (imageData == null || imageData.isEmpty()) {
            throw new IllegalArgumentException("ImageData must not be null or empty");
        }
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData.data()));
            if (img == null) {
                throw new IllegalArgumentException("Unsupported or corrupt image data");
            }
            // Use FaceBytes directly
            List<com.deepface.core.EmbeddingResult> ers = com.deepface.core.DeepFace.represent(img);
            List<float[]> out = new ArrayList<>(ers.size());
            for (com.deepface.core.EmbeddingResult er : ers) {
                float[] vec = er.embedding();
                out.add(l2Normalize(vec));
            }
            return out;
        } catch (Exception e) {
            throw new BaseVisionException("Failed to extract embeddings: " + e.getMessage(), e) {};
        }
    }

    public static boolean defaultVerify(ImageData a, ImageData b, String metric, double threshold) throws BaseVisionException {
        List<float[]> ea = defaultExtractEmbeddings(a);
        List<float[]> eb = defaultExtractEmbeddings(b);
        if (ea.isEmpty() || eb.isEmpty()) return false;
        // Compare top-1 to top-1 by default
        float[] va = ea.get(0);
        float[] vb = eb.get(0);
        double distance = "euclidean".equalsIgnoreCase(metric) ? euclideanDistance(va, vb) : cosineDistance(va, vb);
        return distance <= threshold;
    }

    private static float[] l2Normalize(float[] vec) {
        if (vec == null || vec.length == 0) return vec;
        double s = 0.0; for (float v : vec) s += v * v; s = Math.sqrt(s);
        if (s <= 0) return vec;
        float[] out = new float[vec.length];
        for (int i = 0; i < vec.length; i++) out[i] = (float) (vec[i] / s);
        return out;
    }

    private static double cosineDistance(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return Double.NaN;
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) { dot += a[i] * b[i]; na += a[i]*a[i]; nb += b[i]*b[i]; }
        if (na <= 0 || nb <= 0) return Double.NaN;
        double sim = dot / (Math.sqrt(na) * Math.sqrt(nb));
        return 1.0 - sim;
    }

    private static double euclideanDistance(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return Double.NaN;
        double s = 0.0; for (int i = 0; i < a.length; i++) { double d = a[i] - b[i]; s += d * d; }
        return Math.sqrt(s);
    }
} 