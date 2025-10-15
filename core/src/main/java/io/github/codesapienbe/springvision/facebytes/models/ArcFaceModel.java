package io.github.codesapienbe.springvision.facebytes.models;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;

import io.github.codesapienbe.springvision.facebytes.config.DeepFaceConfig;
import io.github.codesapienbe.springvision.facebytes.exceptions.DeepFaceException;
import io.github.codesapienbe.springvision.facebytes.utils.Logs;

/**
 * ArcFace embedding model wrapper. Uses ONNX Runtime via ModelManager when available
 * with InsightFace-style preprocessing. When the ONNX model is not available this
 * class will fail fast and provide actionable guidance to configure or download
 * the required model artifact.
 */
public final class ArcFaceModel {

    /**
     * Default input size for ArcFace model.
     */
    public static final int DEFAULT_INPUT_SIZE = 112;

    /**
     * Creates a new ArcFaceModel instance.
     */
    public ArcFaceModel() {
    }

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
        try {
            float[] onnx = tryOnnxEmbedding(resized, size);
            if (onnx != null) {
                return l2normalize(onnx);
            }
            Logs.error("ArcFaceModel", "onnx.unavailable", null, java.util.Map.of("advice", "Set FACEBYTES_ARCFACE_ONNX_PATH or system property facebytes.arcface.onnx, or enable auto-download via facebytes.auto_download"));
            throw new DeepFaceException("ArcFace ONNX model is not available. Configure the model path via environment variable 'FACEBYTES_ARCFACE_ONNX_PATH' or system property 'facebytes.arcface.onnx', or enable auto-download in configuration.");
        } catch (DeepFaceException e) {
            throw e;
        } catch (Throwable t) {
            Logs.error("ArcFaceModel", "onnx.inference_failed", t, java.util.Map.of());
            throw new DeepFaceException("ArcFace ONNX inference failed: " + t.getMessage(), t);
        }
    }

    private static float[] tryOnnxEmbedding(BufferedImage resized, int size) throws Exception {
        Class<?> mm = Class.forName("io.github.codesapienbe.springvision.facebytes.models.ModelManager");
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

    // Mock embedding removed: ArcFace requires a real ONNX model. Configure model path or enable auto-download.
}
