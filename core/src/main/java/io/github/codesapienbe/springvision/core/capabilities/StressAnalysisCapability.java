package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Capability for analyzing stress levels from facial expressions and body language.
 *
 * <p>This capability estimates stress levels by analyzing multiple visual cues including
 * facial expressions, emotion patterns, and body posture. While not a medical diagnostic tool,
 * it provides useful indicators for user experience research and wellness monitoring.</p>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>User experience (UX) research and testing</li>
 *   <li>Workplace wellness monitoring</li>
 *   <li>Customer service quality assessment</li>
 *   <li>Educational stress management</li>
 *   <li>Mental health screening support (non-diagnostic)</li>
 *   <li>Gaming and entertainment experience optimization</li>
 * </ul>
 *
 * <h2>Analysis Strategy</h2>
 * <p>Stress analysis combines multiple indicators:</p>
 * <ul>
 *   <li><b>Emotion Patterns</b> - Negative emotions (angry, fear, sad) indicate stress</li>
 *   <li><b>Expression Intensity</b> - Facial muscle tension and frowning</li>
 *   <li><b>Temporal Consistency</b> - Sustained negative emotions over time</li>
 *   <li><b>Facial Features</b> - Eyebrow position, eye openness, mouth shape</li>
 *   <li><b>Body Language</b> - Tension in shoulders and posture (if available)</li>
 * </ul>
 *
 * <h2>Detection Attributes</h2>
 * <p>Each Detection contains:</p>
 * <ul>
 *   <li><b>stressLevel</b> (String) - "low", "moderate", "high"</li>
 *   <li><b>stressScore</b> (Double) - Numerical stress estimate (0.0-1.0)</li>
 *   <li><b>confidence</b> (Double) - Analysis confidence (0.0-1.0)</li>
 *   <li><b>dominantEmotion</b> (String) - Primary detected emotion</li>
 *   <li><b>emotionIntensity</b> (Double) - Strength of emotional expression</li>
 *   <li><b>indicators</b> (List) - Specific stress indicators detected</li>
 *   <li><b>frameIndex</b> (Integer) - Frame number (if sequence)</li>
 * </ul>
 *
 * <h2>Implementation Notes</h2>
 * <ul>
 *   <li>Uses existing {@link EmotionDetectionCapability} for emotion analysis</li>
 *   <li>Single image: Analyzes current emotional state and facial features</li>
 *   <li>Image sequence: Tracks emotion patterns and consistency over time</li>
 *   <li>Heuristic-based algorithm, not medical-grade</li>
 *   <li>Should not be used for clinical diagnosis</li>
 * </ul>
 *
 * <h2>Limitations & Ethical Considerations</h2>
 * <p><b>Important:</b> This capability provides general stress indicators and should not be used for:</p>
 * <ul>
 *   <li>Medical diagnosis or clinical assessment</li>
 *   <li>Employment decisions without proper consent</li>
 *   <li>Surveillance or monitoring without explicit user agreement</li>
 *   <li>Any high-stakes decision making</li>
 * </ul>
 * <p>Always obtain informed consent and handle stress data ethically and privately.</p>
 *
 * @author Spring Vision Team
 * @see EmotionDetectionCapability
 * @see FaceDetectionCapability
 * @since 1.0.8
 */
public interface StressAnalysisCapability {

    /**
     * Analyzes stress levels from a list of images (single frame or temporal sequence).
     *
     * <p>For single image: Analyzes current emotional state and facial features.</p>
     * <p>For sequence: Tracks emotion patterns and consistency for more reliable assessment.</p>
     *
     * <p>Returns one Detection per frame/image with label indicating stress level
     * and attributes containing detailed stress analysis metrics.</p>
     *
     * @param imageDataList list of images (single or sequence)
     * @return list of detections with stress analysis for each frame
     * @throws BaseVisionException if analysis fails
     */
    List<Detection> detectStress(List<ImageData> imageDataList) throws BaseVisionException;
}
