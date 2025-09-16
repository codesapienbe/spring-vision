package com.deepface.models;

import com.deepface.exceptions.DeepFaceException;
import com.deepface.utils.Logs;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;

public final class DeepFaceModel {
    public static final int DEFAULT_INPUT_SIZE = 152;

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
            
            Logs.error("DeepFaceModel", "onnx.unavailable", null, java.util.Map.of("advice", "Set FACEBYTES_DEEPFACE_ONNX_PATH or system property facebytes.deepface.onnx, or enable auto-download via facebytes.auto_download"));
            throw new DeepFaceException("DeepFace ONNX model is not available. Configure the model path via 'FACEBYTES_DEEPFACE_ONNX_PATH' or system property 'facebytes.deepface.onnx', or enable auto-download in configuration.");
        } catch (DeepFaceException e) {
            throw e;
        } catch (Throwable t) {
            Logs.error("DeepFaceModel", "onnx.inference_failed", t, java.util.Map.of());
            throw new DeepFaceException("DeepFace embedding inference failed", t);
        }
    }

    private static float[] tryOnnxEmbedding(BufferedImage resized, int size) throws Exception {
        Class<?> mm = Class.forName("com.deepface.models.ModelManager");
        var isAvailable = mm.getMethod("isDeepFaceAvailable");
        boolean available = (boolean) isAvailable.invoke(null);
        if (!available) return null;
        float[] nchw = toNchwBgrMean(resized);
        long[] shape = new long[]{1, 3, size, size};
        var run = mm.getMethod("runDeepFaceEmbedding", float[].class, long[].class);
        return (float[]) run.invoke(null, nchw, shape);
    }

    private static BufferedImage resize(BufferedImage img, int w, int h) { BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB); Graphics2D g = out.createGraphics(); g.drawImage(img, 0, 0, w, h, null); g.dispose(); return out; }

    private static float[] toNchwBgrMean(BufferedImage img) {
        final float meanB = 93.5940f, meanG = 104.7624f, meanR = 129.1863f;
        int w = img.getWidth(), h = img.getHeight(); float[] out = new float[1 * 3 * h * w]; int c = h * w;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int rgb = img.getRGB(x, y); int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = (rgb) & 0xFF; int idx = y * w + x;
            out[idx] = b - meanB; out[c + idx] = g - meanG; out[2 * c + idx] = r - meanR;
        }
        return out;
    }

    private static float[] l2normalize(float[] v) { double n = 0.0; for (float f : v) n += f * f; n = Math.sqrt(n); if (n > 0) { float inv = (float)(1.0 / n); for (int i = 0; i < v.length; i++) v[i] *= inv; } return v; }
}
