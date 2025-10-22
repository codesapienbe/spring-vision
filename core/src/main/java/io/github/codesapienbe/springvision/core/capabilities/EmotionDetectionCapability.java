package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

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
 * <h2>Detection Attributes</h2>
 * <p>Each Detection contains:</p>
 * <ul>
 *   <li><b>emotion</b> (String) - The emotion label</li>
 *   <li><b>faceIndex</b> (Integer) - Index of the face</li>
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
     * <p>Returns one Detection per face with label being the detected emotion
     * (e.g., "happy", "sad") and confidence score. The detection attributes contain
     * the emotion string and faceIndex.</p>
     *
     * @param imageData the image to analyze
     * @return list of detections, one per face, with emotion as label
     * @throws BaseVisionException if detection fails
     */
    List<Detection> detectEmotions(ImageData imageData) throws BaseVisionException;
}
