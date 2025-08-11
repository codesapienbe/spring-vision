package com.deepface.models;

import com.deepface.exceptions.DeepFaceException;
import com.deepface.utils.Logs;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.SplittableRandom;

public final class VGGFaceModel {

    public static final int EMBEDDING_SIZE = 512;

    public VGGFaceModel() {}

    public float[] generateEmbedding(BufferedImage face) {
        return generateEmbedding(face, 224);
    }

    public float[] generateEmbedding(BufferedImage face, int targetSize) {
        int size = Math.max(32, targetSize);
        BufferedImage input = face != null ? face : new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        BufferedImage resized = resize(input, size, size);
        try {
            float[] onnx = tryOnnxEmbedding(resized, size);
            if (onnx != null) {
                return l2normalize(onnx);
            }
            Logs.warn("VGGFaceModel", "onnx.unavailable", java.util.Map.of());
            throw new DeepFaceException("VGGFace ONNX session not available");
        } catch (DeepFaceException e) {
            throw e;
        } catch (Throwable t) {
            Logs.error("VGGFaceModel", "onnx.inference_failed", t, java.util.Map.of());
            throw new DeepFaceException("VGGFace embedding inference failed", t);
        }
    }

    private static float[] tryOnnxEmbedding(BufferedImage resized, int size) throws Exception {
        Class<?> mm = Class.forName("com.deepface.models.ModelManager");
        Method isAvailable = mm.getMethod("isVggFaceAvailable");
        boolean available = (boolean) isAvailable.invoke(null);
        if (!available) return null;
        // VGGFace ONNX typically expects BGR channel order with mean subtraction (no /255 scaling)
        float[] nchw = toNchwVggBgrMean(resized);
        long[] shape = new long[]{1, 3, size, size};
        Method run = mm.getMethod("runVggFaceEmbedding", float[].class, long[].class);
        return (float[]) run.invoke(null, nchw, shape);
    }

    private static BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    // Default NCHW RGB [0,1] used for generic models (kept for fallback/mock)
    private static float[] toNchw(BufferedImage img) {
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
                out[0 * cStride + idx] = r / 255.0f;
                out[1 * cStride + idx] = g / 255.0f;
                out[2 * cStride + idx] = b / 255.0f;
            }
        }
        return out;
    }

    // VGGFace-specific preprocessing: BGR order with mean subtraction per channel (no scaling)
    private static float[] toNchwVggBgrMean(BufferedImage img) {
        final float meanB = 93.5940f;
        final float meanG = 104.7624f;
        final float meanR = 129.1863f;
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
                out[0 * cStride + idx] = (float) b - meanB;
                out[1 * cStride + idx] = (float) g - meanG;
                out[2 * cStride + idx] = (float) r - meanR;
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
}
