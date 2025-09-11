package com.deepface.models;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class DlibModel {
    public static final int DEFAULT_INPUT_SIZE = 150;

    public float[] generateEmbedding(BufferedImage face) { return generateEmbedding(face, DEFAULT_INPUT_SIZE); }

    public float[] generateEmbedding(BufferedImage face, int targetSize) {
        int size = Math.max(32, targetSize);
        BufferedImage input = face != null ? face : new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        BufferedImage resized = resize(input, size, size);
        try {
            // Create a deterministic seed from image content for stable mock embeddings
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            int w = resized.getWidth();
            int h = resized.getHeight();
            int stepY = Math.max(1, h / 16);
            int stepX = Math.max(1, w / 16);
            for (int y = 0; y < h; y += stepY) {
                for (int x = 0; x < w; x += stepX) {
                    int rgb = resized.getRGB(x, y);
                    md.update((byte) (rgb >> 16));
                    md.update((byte) (rgb >> 8));
                    md.update((byte) (rgb));
                }
            }
            byte[] dig = md.digest();
            long seed = 0L;
            for (int i = 0; i < Math.min(8, dig.length); i++) seed = (seed << 8) ^ (dig[i] & 0xFF);
            if (seed == 0L) seed = 1L;

            java.util.SplittableRandom rnd = new java.util.SplittableRandom(seed);
            final int dim = 128; // typical DLIB embedding size
            float[] emb = new float[dim];
            for (int i = 0; i < dim; i++) {
                emb[i] = (float) rnd.nextDouble(-1.0, 1.0);
            }
            return l2normalize(emb);
        } catch (Exception e) {
            // Fallback deterministic embedding based on hashCode
            int hash = resized.hashCode();
            final int dim = 128;
            float[] emb = new float[dim];
            for (int i = 0; i < dim; i++) emb[i] = (float) Math.sin(hash + i * 0.1);
            return l2normalize(emb);
        }
    }

    private static BufferedImage resize(BufferedImage img, int w, int h) { BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB); Graphics2D g = out.createGraphics(); g.drawImage(img, 0, 0, w, h, null); g.dispose(); return out; }

    private static float[] l2normalize(float[] v) { double n = 0.0; for (float f : v) n += f * f; n = Math.sqrt(n); if (n > 0) { float inv = (float)(1.0 / n); for (int i = 0; i < v.length; i++) v[i] *= inv; } return v; }
}
