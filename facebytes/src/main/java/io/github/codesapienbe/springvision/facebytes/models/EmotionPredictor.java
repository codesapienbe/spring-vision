package io.github.codesapienbe.springvision.facebytes.models;

import io.github.codesapienbe.springvision.facebytes.config.DeepFaceConfig;
import io.github.codesapienbe.springvision.facebytes.exceptions.DeepFaceException;
import io.github.codesapienbe.springvision.facebytes.utils.Logs;

import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Emotion prediction model for facial analysis.
 * Provides emotion recognition from face images using ONNX models with fail-fast error handling.
 *
 * @author FaceBytes Team
 * @since 1.0.0
 */
public final class EmotionPredictor {

    /**
     * Represents different emotional states that can be detected.
     */
    public enum Emotion {
        /**
         * Happy emotion
         */
        HAPPY,
        /**
         * Sad emotion
         */
        SAD,
        /**
         * Angry emotion
         */
        ANGRY,
        /**
         * Surprised emotion
         */
        SURPRISED,
        /**
         * Fearful emotion
         */
        FEARFUL,
        /**
         * Disgusted emotion
         */
        DISGUSTED,
        /**
         * Neutral emotion
         */
        NEUTRAL
    }

    private static final int DEFAULT_INPUT_SIZE = 224;
    private static final double CONFIDENCE_THRESHOLD = 0.3;

    private final DeepFaceConfig config;
    private final int inputSize;

    /**
     * Default constructor for EmotionPredictor.
     */
    public EmotionPredictor() {
        this.config = DeepFaceConfig.current();
        this.inputSize = config.emotionInputSize();
    }

    /**
     * Constructs an EmotionPredictor with the given configuration.
     * @param config The configuration to use.
     */
    public EmotionPredictor(DeepFaceConfig config) {
        this.config = config;
        this.inputSize = config.emotionInputSize();
    }

    /**
     * Predicts the emotion from a face image.
     *
     * @param face the face image to analyze
     * @return predicted emotion with confidence
     * @throws DeepFaceException if emotion prediction fails
     */
    public EmotionResult predictEmotion(BufferedImage face) throws DeepFaceException {
        if (face == null) {
            throw new IllegalArgumentException("Face image cannot be null");
        }

        try {
            // Try real ONNX model first
            EmotionResult onnxResult = tryOnnxEmotionPrediction(face);
            if (onnxResult != null) return onnxResult;
            Logs.error("EmotionPredictor", "onnx.unavailable", null, Map.of("advice", "Set FACEBYTES_EMOTION_ONNX_PATH or enable auto-download"));
            throw new DeepFaceException("Emotion ONNX model is not available. Configure 'FACEBYTES_EMOTION_ONNX_PATH' or enable auto-download.");

        } catch (DeepFaceException e) {
            throw e;
        } catch (Throwable t) {
            Logs.error("EmotionPredictor", "emotion.prediction_failed", t, Map.of("input_size", inputSize));
            throw new DeepFaceException("Emotion prediction failed", t);
        }
    }

    /**
     * Attempts to run ONNX inference for emotion prediction.
     *
     * @param face the face image
     * @return emotion prediction result or null if ONNX not available
     * @throws Exception if ONNX inference fails
     */
    private EmotionResult tryOnnxEmotionPrediction(BufferedImage face) throws Exception {
        // Emotion ONNX model is not available in the current implementation
        // Return null to indicate unavailability, which will trigger the fail-fast exception
        return null;
    }

    /**
     * Preprocesses face image for emotion prediction model.
     *
     * @param face the input face image
     * @return preprocessed image
     */
    private BufferedImage preprocessForEmotion(BufferedImage face) {
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
     * Interprets the ONNX output to determine emotion and confidence.
     *
     * @param output the model output array
     * @return emotion prediction result
     */
    private EmotionResult interpretEmotionOutput(float[] output) {
        if (output == null || output.length < 7) {
            return new EmotionResult(Emotion.NEUTRAL, 0.0);
        }

        // Find the emotion with highest probability
        int maxIndex = 0;
        float maxProb = output[0];

        for (int i = 1; i < output.length; i++) {
            if (output[i] > maxProb) {
                maxProb = output[i];
                maxIndex = i;
            }
        }

        // Map index to emotion (assuming standard emotion order)
        Emotion emotion = mapIndexToEmotion(maxIndex);

        // Check if confidence meets threshold
        if (maxProb > CONFIDENCE_THRESHOLD) {
            return new EmotionResult(emotion, maxProb);
        } else {
            return new EmotionResult(Emotion.NEUTRAL, maxProb);
        }
    }

    /**
     * Maps model output index to emotion enum.
     *
     * @param index the model output index
     * @return corresponding emotion
     */
    private Emotion mapIndexToEmotion(int index) {
        return switch (index) {
            case 0 -> Emotion.HAPPY;
            case 1 -> Emotion.SAD;
            case 2 -> Emotion.ANGRY;
            case 3 -> Emotion.SURPRISED;
            case 4 -> Emotion.FEARFUL;
            case 5 -> Emotion.DISGUSTED;
            case 6 -> Emotion.NEUTRAL;
            default -> Emotion.NEUTRAL;
        };
    }

    // Mock emotion generator removed: real ONNX model required.

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
     * Gets the confidence threshold for emotion classification.
     *
     * @return the confidence threshold
     */
    public double getConfidenceThreshold() {
        return CONFIDENCE_THRESHOLD;
    }

    /**
     * Gets all available emotions.
     *
     * @return array of all emotions
     */
    public Emotion[] getAvailableEmotions() {
        return Emotion.values();
    }

    /**
     * Result class for emotion prediction.
     */
    public static final class EmotionResult {
        private final Emotion emotion;
        private final double confidence;

        /**
         * Constructs a new EmotionResult.
         * @param emotion The predicted emotion.
         * @param confidence The confidence of the prediction.
         */
        public EmotionResult(Emotion emotion, double confidence) {
            this.emotion = emotion;
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        }

        /**
         * @return The predicted emotion.
         */
        public Emotion emotion() {
            return emotion;
        }

        /**
         * @return The confidence of the prediction.
         */
        public double confidence() {
            return confidence;
        }

        @Override
        public String toString() {
            return String.format("EmotionResult{emotion=%s, confidence=%.3f}", emotion, confidence);
        }
    }
}
