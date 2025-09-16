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
        if (face == null) {
            throw new IllegalArgumentException("Face image cannot be null");
        }
        
        int size = Math.max(32, targetSize);
        BufferedImage resized = resize(face, size, size);
        try {
            float[] onnx = tryOnnxEmbedding(resized, size);
            if (onnx != null) return l2normalize(onnx);
            Logs.error("SFaceModel", "onnx.unavailable", null, java.util.Map.of("advice", "Set FACEBYTES_SFACE_ONNX_PATH or enable auto-download"));
            throw new DeepFaceException("SFace ONNX model is not available. Configure 'FACEBYTES_SFACE_ONNX_PATH' or enable auto-download.");
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

    // Mock embedding helper removed: real ONNX model required.
}
