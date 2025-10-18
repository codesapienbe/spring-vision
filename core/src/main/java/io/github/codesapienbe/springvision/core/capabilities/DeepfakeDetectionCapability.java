package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;

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
 * <h2>Detection Attributes</h2>
 * <p>The returned Detection contains:</p>
 * <ul>
 *   <li><b>isFake</b> (Boolean) - Whether image is deepfake</li>
 *   <li><b>classification</b> (String) - Classification label ("real" or "fake")</li>
 *   <li><b>manipulationType</b> (String) - Type of manipulation if detected</li>
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
     * <p>Returns a single Detection with label matching the classification ("real" or "fake")
     * and confidence score. The detection attributes contain isFake boolean, classification
     * string, and manipulationType if applicable.</p>
     *
     * @param imageData the image to analyze
     * @return list with single detection containing deepfake classification
     * @throws BaseVisionException if detection fails
     */
    List<Detection> detectDeepfake(ImageData imageData) throws BaseVisionException;
}

