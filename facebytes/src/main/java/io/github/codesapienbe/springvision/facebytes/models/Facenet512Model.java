package io.github.codesapienbe.springvision.facebytes.models;

import io.github.codesapienbe.springvision.facebytes.exceptions.DeepFaceException;
import io.github.codesapienbe.springvision.facebytes.utils.Logs;

import java.awt.image.BufferedImage;

/**
 * Facenet512 face recognition model implementation.
 * Provides face embedding generation using the Facenet architecture with a 512-dimensional output.
 */
public final class Facenet512Model {
    private final FacenetModel delegate = new FacenetModel();

    /**
     * Default constructor for Facenet512Model.
     */
    public Facenet512Model() {
        // Default constructor
    }

    /**
     * Generates a 512-dimensional face embedding using the default input size.
     * @param face The face image.
     * @return The face embedding vector.
     */
    public float[] generateEmbedding(BufferedImage face) {
        return generateEmbedding(face, FacenetModel.DEFAULT_INPUT_SIZE);
    }

    /**
     * Generates a 512-dimensional face embedding with a specified input size.
     * @param face The face image.
     * @param targetSize The target input size for the model.
     * @return The face embedding vector.
     */
    public float[] generateEmbedding(BufferedImage face, int targetSize) {
        float[] v = delegate.generateEmbedding(face, targetSize);
        if (v == null || v.length != 512) {
            Logs.error("Facenet512Model", "onnx.unavailable_or_invalid_dim", null, java.util.Map.of("len", v == null ? -1 : v.length));
            throw new io.github.codesapienbe.springvision.facebytes.exceptions.DeepFaceException("Facenet512 ONNX model is not available or invalid dimension. Configure a proper Facenet512 ONNX model via configuration.");
        }
        return v;
    }
}
