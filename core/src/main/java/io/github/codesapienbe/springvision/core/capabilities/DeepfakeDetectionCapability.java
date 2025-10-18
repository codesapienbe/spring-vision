package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.Map;

/**
 * Capability for detecting deepfake/manipulated images.
 *
 * <p>This capability provides detection of synthetically generated or manipulated
 * images using deep learning techniques.</p>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Media verification and authentication</li>
 *   <li>Fraud detection and prevention</li>
 *   <li>Content moderation</li>
 *   <li>Journalism and fact-checking</li>
 *   <li>Security and surveillance</li>
 * </ul>
 *
 * <h2>Verified Models</h2>
 * <ul>
 *   <li><b>prithivMLmods/deepfake-detector-model-v1</b> - SigLIP-based with 94.44% accuracy</li>
 *   <li><b>mhamza-007/cvit_deepfake_detection</b> - Convolutional Vision Transformer for video frames</li>
 *   <li><b>Naman712/Deep-fake-detection</b> - Alternative detection model</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @see FaceDetectionCapability
 * @since 1.0.8
 */
public interface DeepfakeDetectionCapability {

    /**
     * Detects whether an image is a deepfake or authentic.
     *
     * @param imageData the image to analyze
     * @return deepfake detection result with classification and confidence
     * @throws BaseVisionException if detection fails
     */
    DeepfakeResult detectDeepfake(ImageData imageData) throws BaseVisionException;

    /**
     * Result of deepfake detection.
     *
     * @param isFake         whether the image is detected as a deepfake
     * @param confidence     confidence score (0.0 to 1.0)
     * @param classification the classification label (e.g., "real", "fake")
     * @param manipulationType type of manipulation detected, if applicable
     * @param attributes     additional attributes like model name, detection details, etc.
     */
    record DeepfakeResult(
        boolean isFake,
        double confidence,
        String classification,
        String manipulationType,
        Map<String, Object> attributes
    ) {
    }
}

