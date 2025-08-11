package com.deepface.detectors;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.deepface.config.DeepFaceConfig;
import com.deepface.core.FaceRegion;
import com.deepface.utils.Logs;
import com.deepface.utils.Nms;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RetinaFace ONNX detector. Uses a common 640x640 input variant with outputs:
 * - scores: [1, N, 2] or [1, N] (sigmoid)
 * - boxes: [1, N, 4] in center-form or delta decoded by anchors (implementation assumes directly predicted boxes in xyxy normalized)
 *
 * Note: There are multiple RetinaFace ONNX variants. This implementation targets a typical lightweight export that
 * returns boxes in absolute pixel coordinates for the model input size. If your model differs, adjust decode logic.
 */
public final class RetinaFaceDetector implements FaceDetector {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final String inputName;

    public RetinaFaceDetector() {
        try {
            String modelPath = DeepFaceConfig.current().retinaFaceOnnxPath();
            if (modelPath == null || modelPath.isBlank()) {
                Logs.warn("RetinaFaceDetector", "onnx.missing", Map.of());
                // Fallback to OpenCV if not configured
                this.env = null; this.session = null; this.inputName = null;
                return;
            }
            this.env = OrtEnvironment.getEnvironment();
            this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
            this.inputName = session.getInputNames().iterator().next();
            Logs.info("RetinaFaceDetector", "onnx.loaded", Map.of("path", modelPath));
        } catch (Exception e) {
            Logs.error("RetinaFaceDetector", "onnx.init_failed", e, Map.of());
            throw new IllegalStateException("Failed to initialize RetinaFace ONNX", e);
        }
    }

    @Override
    public List<FaceRegion> detectFaces(BufferedImage image) {
        if (image == null) return List.of();
        if (session == null) {
            // Fallback to OpenCV when ONNX is missing
            return new OpenCVDetector().detectFaces(image);
        }
        DeepFaceConfig cfg = DeepFaceConfig.current();
        int size = cfg.retinaFaceInputSize();
        double scoreThr = cfg.retinaFaceScoreThreshold();
        double nmsThr = cfg.retinaFaceNmsThreshold();
        try {
            // Preprocess: letterbox-resize to square
            Preprocessed prep = letterbox(image, size, size);
            float[] nchw = toNchw01(prep.resized);
            long[] shape = new long[]{1, 3, size, size};
            try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
                OrtSession.Result res = session.run(Collections.singletonMap(inputName, input));
                Map<String, OnnxValue> outputs = new LinkedHashMap<>();
                for (Map.Entry<String, OnnxValue> e : res) outputs.put(e.getKey(), e.getValue());
                // Heuristic output parsing: look for boxes and scores tensors
                float[][] boxes = first2dFloat(outputs, 4); // [N,4]
                float[] scores = first1dFloat(outputs);     // [N]
                if (boxes == null || scores == null || boxes.length != scores.length) {
                    Logs.warn("RetinaFaceDetector", "onnx.unexpected_output", Map.of());
                    return List.of();
                }
                // Filter by score and build boxes in original image coords via reverse letterbox
                List<Nms.Box> nmsBoxes = new ArrayList<>();
                List<int[]> rawBoxes = new ArrayList<>();
                for (int i = 0; i < scores.length; i++) {
                    float sc = sigmoid(scores[i]);
                    if (sc < scoreThr) continue;
                    float x1 = boxes[i][0];
                    float y1 = boxes[i][1];
                    float x2 = boxes[i][2];
                    float y2 = boxes[i][3];
                    // Model coordinates assumed in resized image space
                    float[] xyxy = invertLetterbox(x1, y1, x2, y2, prep, image.getWidth(), image.getHeight());
                    nmsBoxes.add(new Nms.Box(xyxy[0], xyxy[1], xyxy[2], xyxy[3], sc));
                }
                // Apply NMS
                List<Integer> keep = Nms.nms(nmsBoxes, nmsThr, 200);
                List<FaceRegion> out = new ArrayList<>(keep.size());
                for (int idx : keep) {
                    Nms.Box b = nmsBoxes.get(idx);
                    int x = Math.max(0, Math.round(b.x1));
                    int y = Math.max(0, Math.round(b.y1));
                    int w = Math.min(image.getWidth() - x, Math.max(0, Math.round(b.x2 - b.x1)));
                    int h = Math.min(image.getHeight() - y, Math.max(0, Math.round(b.y2 - b.y1)));
                    if (w > 0 && h > 0) out.add(new FaceRegion(x, y, w, h, (double) b.score, null));
                }
                res.close();
                return out;
            }
        } catch (Exception e) {
            Logs.error("RetinaFaceDetector", "detect.failed", e, Map.of());
            return List.of();
        }
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

    private static class Preprocessed {
        final BufferedImage resized; final int padX; final int padY; final float scale;
        Preprocessed(BufferedImage r, int px, int py, float s) { this.resized = r; this.padX = px; this.padY = py; this.scale = s; }
    }

    private static Preprocessed letterbox(BufferedImage img, int dstW, int dstH) {
        int srcW = img.getWidth(); int srcH = img.getHeight();
        float scale = Math.min(dstW / (float) srcW, dstH / (float) srcH);
        int newW = Math.round(srcW * scale);
        int newH = Math.round(srcH * scale);
        BufferedImage out = new BufferedImage(dstW, dstH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(new java.awt.Color(114, 114, 114));
        g.fillRect(0, 0, dstW, dstH);
        int padX = (dstW - newW) / 2;
        int padY = (dstH - newH) / 2;
        g.drawImage(img, padX, padY, padX + newW, padY + newH, 0, 0, srcW, srcH, null);
        g.dispose();
        return new Preprocessed(out, padX, padY, scale);
    }

    private static float[] invertLetterbox(float x1, float y1, float x2, float y2, Preprocessed prep, int origW, int origH) {
        // Map from resized letterboxed coords back to original image coords
        float xx1 = (x1 - prep.padX) / prep.scale;
        float yy1 = (y1 - prep.padY) / prep.scale;
        float xx2 = (x2 - prep.padX) / prep.scale;
        float yy2 = (y2 - prep.padY) / prep.scale;
        xx1 = clamp(xx1, 0, origW - 1);
        yy1 = clamp(yy1, 0, origH - 1);
        xx2 = clamp(xx2, 0, origW - 1);
        yy2 = clamp(yy2, 0, origH - 1);
        return new float[]{xx1, yy1, xx2, yy2};
    }

    private static float clamp(float v, int lo, int hi) {
        if (v < lo) return lo; if (v > hi) return hi; return v;
    }

    private static float sigmoid(float x) {
        double e = Math.exp(-x);
        return (float) (1.0 / (1.0 + e));
    }

    private static float[][] first2dFloat(Map<String, OnnxValue> outputs, int lastDim) throws OrtException {
        for (Map.Entry<String, OnnxValue> e : outputs.entrySet()) {
            OnnxValue v = e.getValue();
            if (v instanceof OnnxTensor t) {
                Object val = t.getValue();
                if (val instanceof float[][] arr) {
                    if (lastDim <= 0 || (arr.length > 0 && arr[0].length == lastDim) || true) {
                        return arr;
                    }
                } else if (val instanceof float[][][] arr3 && arr3.length > 0) {
                    return arr3[0];
                }
            }
        }
        return null;
    }

    private static float[] first1dFloat(Map<String, OnnxValue> outputs) throws OrtException {
        for (Map.Entry<String, OnnxValue> e : outputs.entrySet()) {
            OnnxValue v = e.getValue();
            if (v instanceof OnnxTensor t) {
                Object val = t.getValue();
                if (val instanceof float[] a) return a;
                if (val instanceof float[][] a2 && a2.length > 0 && a2[0].length == 1) {
                    float[] r = new float[a2.length];
                    for (int i = 0; i < a2.length; i++) r[i] = a2[i][0];
                    return r;
                }
            }
        }
        return null;
    }
}
