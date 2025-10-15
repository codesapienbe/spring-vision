package io.github.codesapienbe.springvision.facebytes.models;

import io.github.codesapienbe.springvision.facebytes.exceptions.DeepFaceException;
import io.github.codesapienbe.springvision.facebytes.utils.Logs;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;

/**
 * Facenet face recognition model implementation.
 * Provides face embedding generation using the Facenet architecture.
 */
public final class FacenetModel {
    /**
     * Default input size for the Facenet model.
     */
    public static final int DEFAULT_INPUT_SIZE = 160;

    /**
     * Default constructor for FacenetModel.
     */
    public FacenetModel() {
        // Default constructor
    }

    /**
     * Generates a face embedding using the default input size.
     * @param face The face image.
     * @return The face embedding vector.
     */
    public float[] generateEmbedding(BufferedImage face) {
        return generateEmbedding(face, DEFAULT_INPUT_SIZE);
    }

    /**
     * Generates a face embedding with a specified input size.
     * @param face The face image.
     * @param targetSize The target input size for the model.
     * @return The face embedding vector.
     */
    public float[] generateEmbedding(BufferedImage face, int targetSize) {
        if (face == null) {
            throw new IllegalArgumentException("Face image cannot be null");
        }

        int size = Math.max(32, targetSize);
        BufferedImage resized = resize(face, size, size);
        try {
            float[] onnx = tryOnnxEmbedding(resized, size);
            if (onnx != null) return l2normalize(onnx);

            Logs.error("FacenetModel", "onnx.unavailable", null, java.util.Map.of("advice", "Set FACEBYTES_FACENET_ONNX_PATH or system property facebytes.facenet.onnx, or enable auto-download via facebytes.auto_download"));
            throw new DeepFaceException("Facenet ONNX model is not available. Configure the model path via 'FACEBYTES_FACENET_ONNX_PATH' or system property 'facebytes.facenet.onnx', or enable auto-download in configuration.");
        } catch (DeepFaceException e) {
            throw e;
        } catch (Throwable t) {
            Logs.error("FacenetModel", "onnx.inference_failed", t, java.util.Map.of());
            throw new DeepFaceException("Facenet embedding inference failed", t);
        }
    }

    private static float[] tryOnnxEmbedding(BufferedImage resized, int size) throws Exception {
        Class<?> mm = Class.forName("io.github.codesapienbe.springvision.facebytes.models.ModelManager");
        Method isAvailable = mm.getMethod("isFacenetAvailable");
        boolean available = (boolean) isAvailable.invoke(null);
        if (!available) return null;
        float[] nchw = toNchwStandardized(resized);
        long[] shape = new long[]{1, 3, size, size};
        Method run = mm.getMethod("runFacenetEmbedding", float[].class, long[].class);
        return (float[]) run.invoke(null, nchw, shape);
    }

    private static BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    // Facenet preprocessing: scale to [0,1], then per-image standardization (mean 0, std 1)
    private static float[] toNchwStandardized(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        float[] out = new float[1 * 3 * h * w];
        int cStride = h * w;
        double mean = 0.0, sq = 0.0;
        int n = h * w * 3;
        float[] tmp = new float[n];
        int k = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                float r = ((rgb >> 16) & 0xFF) / 255.0f;
                float g = ((rgb >> 8) & 0xFF) / 255.0f;
                float b = (rgb & 0xFF) / 255.0f;
                tmp[k++] = r;
                tmp[k++] = g;
                tmp[k++] = b;
            }
        }
        for (int i = 0; i < n; i++) mean += tmp[i];
        mean /= n;
        for (int i = 0; i < n; i++) {
            double d = tmp[i] - mean;
            sq += d * d;
        }
        double std = Math.sqrt(sq / Math.max(1, n));
        if (std < 1e-6) std = 1.0;
        k = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float r = (float) ((tmp[k++] - mean) / std);
                float g = (float) ((tmp[k++] - mean) / std);
                float b = (float) ((tmp[k++] - mean) / std);
                int idx = y * w + x;
                out[0 * cStride + idx] = r;
                out[1 * cStride + idx] = g;
                out[2 * cStride + idx] = b;
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
