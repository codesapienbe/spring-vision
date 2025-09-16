package com.deepface.models;

import com.deepface.config.DeepFaceConfig;
import com.deepface.exceptions.DeepFaceException;
import com.deepface.utils.Logs;

import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Race prediction model for facial analysis.
 * Provides race/ethnicity classification from face images using ONNX models with fail-fast error handling.
 * 
 * @author FaceBytes Team
 * @since 1.0.0
 */
public final class RacePredictor {

    public enum Race {
        ASIAN, INDIAN, BLACK, WHITE, MIDDLE_EASTERN, LATINO_HISPANIC, UNKNOWN
    }

    private static final int DEFAULT_INPUT_SIZE = 224;
    private static final double CONFIDENCE_THRESHOLD = 0.4;
    
    private final DeepFaceConfig config;
    private final int inputSize;

    public RacePredictor() {
        this.config = DeepFaceConfig.current();
        this.inputSize = config.raceInputSize();
    }

    public RacePredictor(DeepFaceConfig config) {
        this.config = config;
        this.inputSize = config.raceInputSize();
    }

    /**
     * Predicts the race from a face image.
     * 
     * @param face the face image to analyze
     * @return predicted race with confidence
     * @throws DeepFaceException if race prediction fails
     */
    public RaceResult predictRace(BufferedImage face) throws DeepFaceException {
        if (face == null) {
            throw new IllegalArgumentException("Face image cannot be null");
        }

        try {
            // Try real ONNX model first
            RaceResult onnxResult = tryOnnxRacePrediction(face);
            if (onnxResult != null) return onnxResult;
            Logs.error("RacePredictor", "onnx.unavailable", null, Map.of("advice", "Set FACEBYTES_RACE_ONNX_PATH or enable auto-download"));
            throw new DeepFaceException("Race ONNX model is not available. Configure 'FACEBYTES_RACE_ONNX_PATH' or enable auto-download.");

        } catch (DeepFaceException e) {
            throw e;
        } catch (Throwable t) {
            Logs.error("RacePredictor", "race.prediction_failed", t, Map.of("input_size", inputSize));
            throw new DeepFaceException("Race prediction failed", t);
        }
    }

    /**
     * Attempts to run ONNX inference for race prediction.
     * 
     * @param face the face image
     * @return race prediction result or null if ONNX not available
     * @throws Exception if ONNX inference fails
     */
    private RaceResult tryOnnxRacePrediction(BufferedImage face) throws Exception {
        Class<?> mm = Class.forName("com.deepface.models.ModelManager");
        java.lang.reflect.Method isAvailable = mm.getMethod("isRaceModelAvailable");
        boolean available = (boolean) isAvailable.invoke(null);
        
        if (!available) {
            return null;
        }

        // Preprocess face for race model
        BufferedImage preprocessed = preprocessForRace(face);
        
        // Convert to NCHW format
        float[] nchw = toNchwFormat(preprocessed);
        long[] shape = new long[]{1, 3, inputSize, inputSize};
        
        // Run inference
        java.lang.reflect.Method run = mm.getMethod("runRacePrediction", float[].class, long[].class);
        float[] output = (float[]) run.invoke(null, nchw, shape);
        
        // Convert output to race classification
        return interpretRaceOutput(output);
    }

    /**
     * Preprocesses face image for race prediction model.
     * 
     * @param face the input face image
     * @return preprocessed image
     */
    private BufferedImage preprocessForRace(BufferedImage face) {
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
     * Interprets the ONNX output to determine race and confidence.
     * 
     * @param output the model output array
     * @return race prediction result
     */
    private RaceResult interpretRaceOutput(float[] output) {
        if (output == null || output.length < 7) {
            return new RaceResult(Race.UNKNOWN, 0.0);
        }

        // Find the race with highest probability
        int maxIndex = 0;
        float maxProb = output[0];
        
        for (int i = 1; i < output.length; i++) {
            if (output[i] > maxProb) {
                maxProb = output[i];
                maxIndex = i;
            }
        }

        // Map index to race (assuming standard race order)
        Race race = mapIndexToRace(maxIndex);
        
        // Check if confidence meets threshold
        if (maxProb > CONFIDENCE_THRESHOLD) {
            return new RaceResult(race, maxProb);
        } else {
            return new RaceResult(Race.UNKNOWN, maxProb);
        }
    }

    /**
     * Maps model output index to race enum.
     * 
     * @param index the model output index
     * @return corresponding race
     */
    private Race mapIndexToRace(int index) {
        return switch (index) {
            case 0 -> Race.ASIAN;
            case 1 -> Race.INDIAN;
            case 2 -> Race.BLACK;
            case 3 -> Race.WHITE;
            case 4 -> Race.MIDDLE_EASTERN;
            case 5 -> Race.LATINO_HISPANIC;
            case 6 -> Race.UNKNOWN;
            default -> Race.UNKNOWN;
        };
    }

    // Mock race generator removed: real ONNX model required.

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
     * Gets the confidence threshold for race classification.
     * 
     * @return the confidence threshold
     */
    public double getConfidenceThreshold() {
        return CONFIDENCE_THRESHOLD;
    }

    /**
     * Gets all available races.
     * 
     * @return array of all races
     */
    public Race[] getAvailableRaces() {
        return Race.values();
    }

    /**
     * Result class for race prediction.
     */
    public static final class RaceResult {
        private final Race race;
        private final double confidence;

        public RaceResult(Race race, double confidence) {
            this.race = race;
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        }

        public Race race() {
            return race;
        }

        public double confidence() {
            return confidence;
        }

        @Override
        public String toString() {
            return String.format("RaceResult{race=%s, confidence=%.3f}", race, confidence);
        }
    }
} 