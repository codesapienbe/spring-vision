package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;
import java.util.Map;

/**
 * Capability for detecting emotions from facial expressions in images.
 *
 * <p>This capability provides multi-class emotion classification typically including:</p>
 * <ul>
 *   <li>Angry</li>
 *   <li>Disgust</li>
 *   <li>Fear</li>
 *   <li>Happy</li>
 *   <li>Sad</li>
 *   <li>Surprise</li>
 *   <li>Neutral</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Customer sentiment analysis</li>
 *   <li>Mental health monitoring</li>
 *   <li>User experience research</li>
 *   <li>Interactive applications and gaming</li>
 *   <li>Security and surveillance</li>
 * </ul>
 *
 * <h2>Verified Models</h2>
 * <ul>
 *   <li><b>abhilash88/face-emotion-detection</b> - Vision Transformer with 71.55% accuracy on FER2013</li>
 *   <li><b>prithivMLmods/Facial-Emotion-Detection-SigLIP2</b> - SigLIP-based emotion detector</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @see FaceDetectionCapability
 * @since 1.0.8
 */
public interface EmotionDetectionCapability {

    /**
     * Detects emotions from faces in an image.
     *
     * @param imageData the image to analyze
     * @return emotion detection result with detected emotions and confidence scores
     * @throws BaseVisionException if detection fails
     */
    EmotionResult detectEmotions(ImageData imageData) throws BaseVisionException;

    /**
     * Result of emotion detection.
     *
     * @param emotions      list of detected emotions with confidence scores
     * @param topEmotion    the highest confidence emotion
     * @param facesAnalyzed number of faces analyzed
     * @param attributes    additional attributes like model name, threshold used, etc.
     */
    record EmotionResult(
        List<EmotionClassification> emotions,
        String topEmotion,
        int facesAnalyzed,
        Map<String, Object> attributes
    ) {
    }

    /**
     * Individual emotion classification.
     *
     * @param emotion    the emotion label (e.g., "happy", "sad", "angry")
     * @param confidence confidence score (0.0 to 1.0)
     * @param faceIndex  index of the face this emotion applies to (if multiple faces)
     */
    record EmotionClassification(
        String emotion,
        double confidence,
        int faceIndex
    ) {
    }
}

