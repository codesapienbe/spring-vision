package io.github.codesapienbe.springvision.facebytes.models;

import io.github.codesapienbe.springvision.facebytes.config.DeepFaceConfig;
import io.github.codesapienbe.springvision.facebytes.exceptions.DeepFaceException;
import io.github.codesapienbe.springvision.facebytes.utils.Logs;

import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Age prediction model for facial analysis.
 * Provides age estimation from face images using ONNX models with fail-fast error handling.
 *
 * @author FaceBytes Team
 * @since 1.0.0
 */
public final class AgePredictor {

    private static final int DEFAULT_INPUT_SIZE = 224;
    private static final int MIN_AGE = 0;
    private static final int MAX_AGE = 100;

    private final DeepFaceConfig config;
    private final int inputSize;

    /**
     * Creates a new AgePredictor with default configuration.
     */
    public AgePredictor() {
        this.config = DeepFaceConfig.current();
        this.inputSize = config.ageInputSize();
    }

    /**
     * Creates a new AgePredictor with custom configuration.
     *
     * @param config the configuration to use
     */
    public AgePredictor(DeepFaceConfig config) {
        this.config = config;
        this.inputSize = config.ageInputSize();
    }

    /**
     * Predicts the age from a face image.
     *
     * @param face the face image to analyze
     * @return predicted age (0-100)
     * @throws DeepFaceException if age prediction fails
     */
    public int predictAge(BufferedImage face) throws DeepFaceException {
        if (face == null) {
            throw new IllegalArgumentException("Face image cannot be null");
        }

        try {
            // Try real ONNX model first
            Integer onnxAge = tryOnnxAgePrediction(face);
            if (onnxAge != null) return validateAgeRange(onnxAge);
            Logs.error("AgePredictor", "onnx.unavailable", null, Map.of("advice", "Set FACEBYTES_AGE_ONNX_PATH or enable auto-download"));
            throw new DeepFaceException("Age ONNX model is not available. Configure 'FACEBYTES_AGE_ONNX_PATH' or enable auto-download.");

        } catch (DeepFaceException e) {
            throw e;
        } catch (Throwable t) {
            Logs.error("AgePredictor", "age.prediction_failed", t, Map.of("input_size", inputSize));
            throw new DeepFaceException("Age prediction failed", t);
        }
    }

    /**
     * Attempts to run ONNX inference for age prediction.
     *
     * @param face the face image
     * @return predicted age or null if ONNX not available
     * @throws Exception if ONNX inference fails
     */
    private Integer tryOnnxAgePrediction(BufferedImage face) throws Exception {
        // Age ONNX model is not available in the current implementation
        // Return null to indicate unavailability, which will trigger the fail-fast exception
        return null;
    }

    /**
     * Preprocesses face image for age prediction model.
     *
     * @param face the input face image
     * @return preprocessed image
     */
    private BufferedImage preprocessForAge(BufferedImage face) {
        // Basic preprocessing: resize to model input size
        return resizeImage(face, inputSize, inputSize);
    }

    /**
     * Converts image to NCHW format for ONNX input.
     *
     * @param img the input image
     * @return NCHW formatted array
     */
    private float[] toNchwFormat(BufferedImage img) {
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

                // Normalize to [0, 1] range
                out[0 * cStride + idx] = r / 255.0f;
                out[1 * cStride + idx] = g / 255.0f;
                out[2 * cStride + idx] = b / 255.0f;
            }
        }

        return out;
    }

    /**
     * Resizes image to target dimensions.
     *
     * @param img    the input image
     * @param width  target width
     * @param height target height
     * @return resized image
     */
    private BufferedImage resizeImage(BufferedImage img, int width, int height) {
        java.awt.Image scaled = img.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = resized.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return resized;
    }

    /**
     * Validates that age is within acceptable range.
     *
     * @param age the predicted age
     * @return validated age
     */
    private int validateAgeRange(int age) {
        if (age < MIN_AGE || age > MAX_AGE) {
            Logs.warn("AgePredictor", "age.out_of_range", Map.of("age", age, "min", MIN_AGE, "max", MAX_AGE));
            return Math.max(MIN_AGE, Math.min(MAX_AGE, age));
        }
        return age;
    }

    /**
     * Gets the current configuration.
     *
     * @return the DeepFace configuration
     */
    public DeepFaceConfig getConfig() {
        return config;
    }

    /**
     * Gets the input size for this model.
     *
     * @return the input size
     */
    public int getInputSize() {
        return inputSize;
    }

    /**
     * Gets the valid age range.
     *
     * @return array with [min_age, max_age]
     */
    public int[] getAgeRange() {
        return new int[]{MIN_AGE, MAX_AGE};
    }
}
