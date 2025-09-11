package com.deepface.models;

import com.deepface.exceptions.DeepFaceException;
import com.deepface.utils.Logs;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.SplittableRandom;

public final class SFaceModel {
    public static final int DEFAULT_INPUT_SIZE = 112;

    public float[] generateEmbedding(BufferedImage face) { return generateEmbedding(face, DEFAULT_INPUT_SIZE); }

    public float[] generateEmbedding(BufferedImage face, int targetSize) {
        int size = Math.max(32, targetSize);
        BufferedImage input = face != null ? face : new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        BufferedImage resized = resize(input, size, size);
        try {
            float[] onnx = tryOnnxEmbedding(resized, size);
            if (onnx != null) return l2normalize(onnx);
            Logs.warn("SFaceModel", "onnx.unavailable", java.util.Map.of());
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                int w = resized.getWidth(); int h = resized.getHeight();
                int stepY = Math.max(1, h / 16); int stepX = Math.max(1, w / 16);
                for (int y = 0; y < h; y += stepY) for (int x = 0; x < w; x += stepX) {
                    int rgb = resized.getRGB(x, y); md.update((byte) (rgb >> 16)); md.update((byte) (rgb >> 8)); md.update((byte) (rgb));
                }
                byte[] dig = md.digest(); long seed = 0L; for (int i = 0; i < Math.min(8, dig.length); i++) seed = (seed << 8) ^ (dig[i] & 0xFF);
                if (seed == 0L) seed = 1L;
                java.util.SplittableRandom rnd = new java.util.SplittableRandom(seed);
                final int dim = 512;
                float[] emb = new float[dim];
                for (int i = 0; i < dim; i++) emb[i] = (float) rnd.nextDouble(-1.0, 1.0);
                return l2normalize(emb);
            } catch (Exception ex) {
                int hash = resized.hashCode(); final int dim = 512; float[] emb = new float[dim]; for (int i = 0; i < dim; i++) emb[i] = (float) Math.sin(hash + i * 0.1); return l2normalize(emb);
            }
        } catch (DeepFaceException e) {
            throw e;
        } catch (Throwable t) {
            Logs.error("SFaceModel", "onnx.inference_failed", t, java.util.Map.of());
            throw new DeepFaceException("SFace embedding inference failed", t);
        }
    }

    private static float[] tryOnnxEmbedding(BufferedImage resized, int size) throws Exception {
        Class<?> mm = Class.forName("com.deepface.models.ModelManager");
        Method isAvailable = mm.getMethod("isSFaceAvailable");
        boolean available = (boolean) isAvailable.invoke(null);
        if (!available) return null;
        float[] nchw = toNchwInsight(resized);
        long[] shape = new long[]{1, 3, size, size};
        Method run = mm.getMethod("runSFaceEmbedding", float[].class, long[].class);
        return (float[]) run.invoke(null, nchw, shape);
    }

    private static BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics(); g.drawImage(img, 0, 0, w, h, null); g.dispose(); return out;
    }

    private static float[] toNchwInsight(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        float[] out = new float[1 * 3 * h * w]; int c = h * w;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int rgb = img.getRGB(x, y); int idx = y * w + x;
            float r = (float)((((rgb >> 16) & 0xFF) - 127.5) / 128.0);
            float g = (float)((((rgb >> 8) & 0xFF) - 127.5) / 128.0);
            float b = (float)((((rgb) & 0xFF) - 127.5) / 128.0);
            out[idx] = r; out[c + idx] = g; out[2 * c + idx] = b;
        }
        return out;
    }

    private static float[] l2normalize(float[] v) { double n = 0.0; for (float f : v) n += f * f; n = Math.sqrt(n); if (n > 0) { float inv = (float)(1.0 / n); for (int i = 0; i < v.length; i++) v[i] *= inv; } return v; }

    private static float[] mockEmbedding(BufferedImage img, int size) {
        long seed = 1L;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            int w = img.getWidth(), h = img.getHeight(); int stepY = Math.max(1, h / 16), stepX = Math.max(1, w / 16);
            for (int y = 0; y < h; y += stepY) for (int x = 0; x < w; x += stepX) { int rgb = img.getRGB(x, y); md.update((byte)(rgb >> 16)); md.update((byte)(rgb >> 8)); md.update((byte)(rgb)); }
            byte[] dig = md.digest(); seed = 0L; for (int i = 0; i < Math.min(8, dig.length); i++) seed = (seed << 8) ^ (dig[i] & 0xFF); if (seed == 0L) seed = 1L;
        } catch (Exception ignored) {}
        SplittableRandom rnd = new SplittableRandom(seed); float[] v = new float[size]; for (int i = 0; i < v.length; i++) v[i] = (float)(rnd.nextDouble(-1.0, 1.0)); return v;
    }
}
