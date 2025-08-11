package com.deepface.models;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.SplittableRandom;

import com.deepface.config.DeepFaceConfig;
import com.deepface.exceptions.DeepFaceException;
import com.deepface.utils.Logs;

/**
 * ArcFace embedding model wrapper. Uses ONNX Runtime via ModelManager when available,
 * with InsightFace-style preprocessing. Falls back to deterministic mock when unavailable.
 */
public final class ArcFaceModel {

    public static final int DEFAULT_INPUT_SIZE = 112;

    public ArcFaceModel() {}

    public float[] generateEmbedding(BufferedImage face) { return generateEmbedding(face, DEFAULT_INPUT_SIZE); }

    public float[] generateEmbedding(BufferedImage face, int targetSize) {
        int size = Math.max(32, targetSize);
        BufferedImage input = face != null ? face : new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        BufferedImage resized = resize(input, size, size);
        try {
            float[] onnx = tryOnnxEmbedding(resized, size);
            if (onnx != null) {
                return l2normalize(onnx);
            }
            Logs.warn("ArcFaceModel", "onnx.unavailable", java.util.Map.of());
            Logs.warn("ArcFaceModel", "fallback.mock_embedding", java.util.Map.of("reason", "onnx.unavailable"));
            return l2normalize(mockEmbedding(resized));
        } catch (DeepFaceException e) {
            throw e;
        } catch (Throwable t) {
            Logs.error("ArcFaceModel", "onnx.inference_failed", t, java.util.Map.of());
            Logs.warn("ArcFaceModel", "fallback.mock_embedding", java.util.Map.of("reason", "exception"));
            return l2normalize(mockEmbedding(resized));
        }
    }

    private static float[] tryOnnxEmbedding(BufferedImage resized, int size) throws Exception {
        Class<?> mm = Class.forName("com.deepface.models.ModelManager");
        // Ensure model is available (download if needed)
        String configured = DeepFaceConfig.current().arcFaceOnnxPath();
        if (configured == null || configured.isBlank()) {
            // Resolve default arcface url -> cache
            String path = ModelDownloader.resolveOrDownload(null, "arcface.onnx");
            if (path != null) System.setProperty("facebytes.arcface.onnx", path);
        }
        Method isAvailable = mm.getMethod("isArcFaceAvailable");
        boolean available = (boolean) isAvailable.invoke(null);
        if (!available) return null;
        float[] nchw = toNchwInsightFace(resized);
        long[] shape = new long[]{1, 3, size, size};
        Method run = mm.getMethod("runArcFaceEmbedding", float[].class, long[].class);
        return (float[]) run.invoke(null, nchw, shape);
    }

    private static BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    // InsightFace-style preprocessing: RGB with (value - 127.5) / 128 to roughly [-1, 1]
    private static float[] toNchwInsightFace(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        float[] out = new float[1 * 3 * h * w];
        int cStride = h * w;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb) & 0xFF;
                int idx = y * w + x;
                out[0 * cStride + idx] = (float) ((r - 127.5) / 128.0);
                out[1 * cStride + idx] = (float) ((g - 127.5) / 128.0);
                out[2 * cStride + idx] = (float) ((b - 127.5) / 128.0);
            }
        }
        return out;
    }

    private static float[] l2normalize(float[] v) {
        double n = 0.0;
        for (float f : v) n += f * f;
        n = Math.sqrt(n);
        if (n > 0) {
            float inv = (float) (1.0 / n);
            for (int i = 0; i < v.length; i++) v[i] *= inv;
        }
        return v;
    }

    private static float[] mockEmbedding(BufferedImage img) {
        long seed = 1L;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            int w = img.getWidth();
            int h = img.getHeight();
            int stepY = Math.max(1, h / 16);
            int stepX = Math.max(1, w / 16);
            for (int y = 0; y < h; y += stepY) {
                for (int x = 0; x < w; x += stepX) {
                    int rgb = img.getRGB(x, y);
                    md.update((byte) (rgb >> 16));
                    md.update((byte) (rgb >> 8));
                    md.update((byte) (rgb));
                }
            }
            byte[] dig = md.digest();
            seed = 0L;
            for (int i = 0; i < Math.min(8, dig.length); i++) {
                seed = (seed << 8) ^ (dig[i] & 0xFF);
            }
            if (seed == 0L) seed = 1L;
        } catch (Exception ignored) {}
        SplittableRandom rnd = new SplittableRandom(seed);
        float[] v = new float[512];
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) (rnd.nextDouble(-1.0, 1.0));
        }
        return v;
    }
}
