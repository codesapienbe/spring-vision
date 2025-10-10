package io.github.codesapienbe.springvision.facebytes.models;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Dlib face recognition model implementation.
 * Provides face embedding generation using the Dlib ResNet architecture.
 */
public final class DlibModel {
    /**
     * Default input size for Dlib model.
     */
    public static final int DEFAULT_INPUT_SIZE = 150;

    /**
     * Generates face embedding using default input size.
     *
     * @param face the face image
     * @return the face embedding vector
     */
    public float[] generateEmbedding(BufferedImage face) {
        return generateEmbedding(face, DEFAULT_INPUT_SIZE);
    }

    /**
     * Generates face embedding with specified input size.
     *
     * @param face       the face image
     * @param targetSize the target input size for the model
     * @return the face embedding vector
     */
    public float[] generateEmbedding(BufferedImage face, int targetSize) {
        if (face == null) {
            throw new IllegalArgumentException("Face image cannot be null");
        }

        int size = Math.max(32, targetSize);
        BufferedImage resized = resize(face, size, size);
        // DlibModel embedding requires a real model backend. Mocks removed.
        throw new io.github.codesapienbe.springvision.facebytes.exceptions.DeepFaceException("Dlib ONNX model is not available. Provide a real model implementation or configure an ONNX model.");
    }

    private static BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
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
}
