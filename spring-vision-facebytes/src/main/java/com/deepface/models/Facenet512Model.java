package com.deepface.models;

import com.deepface.exceptions.DeepFaceException;
import com.deepface.utils.Logs;

import java.awt.image.BufferedImage;

public final class Facenet512Model {
    private final FacenetModel delegate = new FacenetModel();

    public float[] generateEmbedding(BufferedImage face) { return generateEmbedding(face, FacenetModel.DEFAULT_INPUT_SIZE); }

    public float[] generateEmbedding(BufferedImage face, int targetSize) {
        float[] v = delegate.generateEmbedding(face, targetSize);
        if (v == null || v.length != 512) {
            Logs.error("Facenet512Model", "onnx.unavailable_or_invalid_dim", null, java.util.Map.of("len", v == null ? -1 : v.length));
            throw new com.deepface.exceptions.DeepFaceException("Facenet512 embedding unavailable or invalid dimension. Provide a proper Facenet512 ONNX model via configuration.");
        }
        return v;
    }
}
