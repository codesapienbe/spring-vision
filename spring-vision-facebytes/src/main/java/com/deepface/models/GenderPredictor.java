package com.deepface.models;

import com.deepface.config.DeepFaceConfig;
import com.deepface.exceptions.DeepFaceException;
import com.deepface.utils.Logs;

import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Gender prediction model for facial analysis.
 * Provides gender classification from face images using ONNX models with fail-fast error handling.
 * 
 * @author FaceBytes Team
 * @since 1.0.0
 */
public final class GenderPredictor {

    public enum Gender {
        MALE, FEMALE, UNKNOWN
    }

    private static final int DEFAULT_INPUT_SIZE = 224;
    private static final double CONFIDENCE_THRESHOLD = 0.6;
    
    private final DeepFaceConfig config;
    private final int inputSize;

    public GenderPredictor() {
        this.config = DeepFaceConfig.current();
        this.inputSize = config.genderInputSize();
    }

    public GenderPredictor(DeepFaceConfig config) {
        this.config = config;
        this.inputSize = config.genderInputSize();
    }

    /**
     * Predicts the gender from a face image.
     * 
     * @param face the face image to analyze
     * @return predicted gender with confidence
     * @throws DeepFaceException if gender prediction fails
     */
    public GenderResult predictGender(BufferedImage face) throws DeepFaceException {
        if (face == null) {
            throw new IllegalArgumentException("Face image cannot be null");
        }

        try {
            // Try real ONNX model first
            GenderResult onnxResult = tryOnnxGenderPrediction(face);
            if (onnxResult != null) return onnxResult;
            Logs.error("GenderPredictor", "onnx.unavailable", null, Map.of("advice", "Set FACEBYTES_GENDER_ONNX_PATH or enable auto-download"));
            throw new DeepFaceException("Gender ONNX model is not available. Configure 'FACEBYTES_GENDER_ONNX_PATH' or enable auto-download.");

        } catch (DeepFaceException e) {
            throw e;
        } catch (Throwable t) {
            Logs.error("GenderPredictor", "gender.prediction_failed", t, Map.of("input_size", inputSize));
            throw new DeepFaceException("Gender prediction failed", t);
        }
    }

    /**
     * Attempts to run ONNX inference for gender prediction.
     * 
     * @param face the face image
     * @return gender prediction result or null if ONNX not available
     * @throws Exception if ONNX inference fails
     */
    private GenderResult tryOnnxGenderPrediction(BufferedImage face) throws Exception {
        // Gender ONNX model is not available in the current implementation
        // Return null to indicate unavailability, which will trigger the fail-fast exception
        return null;
    }

    /**
     * Preprocesses face image for gender prediction model.
     * 
     * @param face the input face image
     * @return preprocessed image
     */
    private BufferedImage preprocessForGender(BufferedImage face) {
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
     * @param img the input image
     * @param width target width
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
     * Interprets the ONNX output to determine gender and confidence.
     * 
     * @param output the model output array
     * @return gender prediction result
     */
    private GenderResult interpretGenderOutput(float[] output) {
        if (output == null || output.length < 2) {
            return new GenderResult(Gender.UNKNOWN, 0.0);
        }

        // Assuming output is [male_probability, female_probability]
        float maleProb = output[0];
        float femaleProb = output[1];
        
        // Normalize probabilities
        float total = maleProb + femaleProb;
        if (total > 0) {
            maleProb /= total;
            femaleProb /= total;
        }
        
        // Determine gender based on highest probability
        if (maleProb > femaleProb && maleProb > CONFIDENCE_THRESHOLD) {
            return new GenderResult(Gender.MALE, maleProb);
        } else if (femaleProb > maleProb && femaleProb > CONFIDENCE_THRESHOLD) {
            return new GenderResult(Gender.FEMALE, femaleProb);
        } else {
            return new GenderResult(Gender.UNKNOWN, Math.max(maleProb, femaleProb));
        }
    }

    // Mock gender generator removed: real ONNX model required.

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
     * Gets the confidence threshold for gender classification.
     * 
     * @return the confidence threshold
     */
    public double getConfidenceThreshold() {
        return CONFIDENCE_THRESHOLD;
    }

    /**
     * Result class for gender prediction.
     */
    public static final class GenderResult {
        private final Gender gender;
        private final double confidence;

        public GenderResult(Gender gender, double confidence) {
            this.gender = gender;
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        }

        public Gender gender() {
            return gender;
        }

        public double confidence() {
            return confidence;
        }

        @Override
        public String toString() {
            return String.format("GenderResult{gender=%s, confidence=%.3f}", gender, confidence);
        }
    }
} 