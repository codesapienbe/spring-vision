package com.deepface.models;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.util.SplittableRandom;

public final class DeepIdModel {
    public static final int DEFAULT_INPUT_SIZE = 96;

    public float[] generateEmbedding(BufferedImage face) { return generateEmbedding(face, DEFAULT_INPUT_SIZE); }

    public float[] generateEmbedding(BufferedImage face, int targetSize) {
        if (face == null) {
            throw new IllegalArgumentException("Face image cannot be null");
        }
        
        int size = Math.max(32, targetSize);
        BufferedImage resized = resize(face, size, size);
        throw new com.deepface.exceptions.DeepFaceException("DeepId ONNX model is not available. Configure the model path or provide an ONNX implementation.");
    }

    private static BufferedImage resize(BufferedImage img, int w, int h) { BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB); Graphics2D g = out.createGraphics(); g.drawImage(img, 0, 0, w, h, null); g.dispose(); return out; }

    private static float[] l2normalize(float[] v) { double n = 0.0; for (float f : v) n += f * f; n = Math.sqrt(n); if (n > 0) { float inv = (float)(1.0 / n); for (int i = 0; i < v.length; i++) v[i] *= inv; } return v; }
}
