package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;

/**
 * Capability for detecting demographic information (age, gender) from faces.
 *
 * <p>This capability provides estimation of age and gender from facial features
 * for demographic analysis and personalization.</p>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Demographic analytics and market research</li>
 *   <li>Personalized user experiences</li>
 *   <li>Targeted advertising</li>
 *   <li>Age verification and access control</li>
 *   <li>Customer behavior analysis</li>
 * </ul>
 *
 * <h2>Verified Models</h2>
 * <ul>
 *   <li><b>abhilash88/age-gender-prediction</b> - Vision Transformer with 94.3% gender accuracy, 4.5 years age MAE</li>
 *   <li><b>fanclan/age-gender-model</b> - Alternative age/gender classification model</li>
 * </ul>
 *
 * <h2>Ethical Considerations</h2>
 * <p><b>Important:</b> Use demographic detection responsibly and in compliance with privacy
 * laws and regulations. Obtain proper consent and handle demographic data securely.</p>
 *
 * <h2>Detection Attributes</h2>
 * <p>Each Detection contains the following attributes:</p>
 * <ul>
 *   <li><b>age</b> (Integer) - Estimated age in years</li>
 *   <li><b>ageRange</b> (String) - Age range (e.g., "25-34")</li>
 *   <li><b>gender</b> (String) - Estimated gender ("Male", "Female")</li>
 *   <li><b>genderConfidence</b> (Double) - Gender confidence (0.0-1.0)</li>
 *   <li><b>ageError</b> (Double) - Estimated age MAE</li>
 *   <li><b>faceIndex</b> (Integer) - Index of the detected face</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @see FaceDetectionCapability
 * @since 1.0.8
 */
public interface DemographicsCapability {

    /**
     * Detects demographic information from faces in an image.
     *
     * <p>Returns one Detection per face with label "{gender}" and attributes containing
     * age, ageRange, gender, genderConfidence, ageError, and faceIndex.</p>
     *
     * @param imageData the image to analyze
     * @return list of detections, one per face, with demographic attributes
     * @throws BaseVisionException if detection fails
     */
    List<Detection> detectDemographics(ImageData imageData) throws BaseVisionException;
}

