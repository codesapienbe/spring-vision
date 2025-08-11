package com.deepface.models;

import com.deepface.exceptions.DeepFaceException;
import com.deepface.utils.Logs;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;

public final class OpenFaceModel {
    public static final int DEFAULT_INPUT_SIZE = 96;

    public float[] generateEmbedding(BufferedImage face) { return generateEmbedding(face, DEFAULT_INPUT_SIZE); }

    public float[] generateEmbedding(BufferedImage face, int targetSize) {
        int size = Math.max(32, targetSize);
        BufferedImage input = face != null ? face : new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        BufferedImage resized = resize(input, size, size);
        try {
            float[] onnx = tryOnnxEmbedding(resized, size);
            if (onnx != null) return l2normalize(onnx);
            Logs.warn("OpenFaceModel", "onnx.unavailable", java.util.Map.of());
            throw new DeepFaceException("OpenFace ONNX session not available");
        } catch (DeepFaceException e) {
            throw e;
        } catch (Throwable t) {
            Logs.error("OpenFaceModel", "onnx.inference_failed", t, java.util.Map.of());
            throw new DeepFaceException("OpenFace embedding inference failed", t);
        }
    }

    private static float[] tryOnnxEmbedding(BufferedImage resized, int size) throws Exception {
        Class<?> mm = Class.forName("com.deepface.models.ModelManager");
        var isAvailable = mm.getMethod("isOpenFaceAvailable");
        boolean available = (boolean) isAvailable.invoke(null);
        if (!available) return null;
        float[] nchw = toNchw01(resized);
        long[] shape = new long[]{1, 3, size, size};
        var run = mm.getMethod("runOpenFaceEmbedding", float[].class, long[].class);
        return (float[]) run.invoke(null, nchw, shape);
    }

    private static BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics(); g.drawImage(img, 0, 0, w, h, null); g.dispose(); return out;
    }

    private static float[] toNchw01(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        float[] out = new float[1 * 3 * h * w]; int c = h * w;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int rgb = img.getRGB(x, y);
            int idx = y * w + x;
            out[idx] = ((rgb >> 16) & 0xFF) / 255.0f;
            out[c + idx] = ((rgb >> 8) & 0xFF) / 255.0f;
            out[2 * c + idx] = (rgb & 0xFF) / 255.0f;
        }
        return out;
    }

    private static float[] l2normalize(float[] v) {
        double n = 0.0; for (float f : v) n += f * f; n = Math.sqrt(n);
        if (n > 0) { float inv = (float)(1.0 / n); for (int i = 0; i < v.length; i++) v[i] *= inv; }
        return v;
    }
}
