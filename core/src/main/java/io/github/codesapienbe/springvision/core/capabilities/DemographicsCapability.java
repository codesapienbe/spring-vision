package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;
import java.util.Map;

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
 * @author Spring Vision Team
 * @see FaceDetectionCapability
 * @since 1.0.8
 */
public interface DemographicsCapability {

    /**
     * Detects demographic information from faces in an image.
     *
     * @param imageData the image to analyze
     * @return demographic detection result with age and gender estimates
     * @throws BaseVisionException if detection fails
     */
    DemographicsResult detectDemographics(ImageData imageData) throws BaseVisionException;

    /**
     * Result of demographic detection.
     *
     * @param demographics list of demographic information for each detected face
     * @param facesAnalyzed number of faces analyzed
     * @param attributes additional attributes like model name, confidence thresholds, etc.
     */
    record DemographicsResult(
        List<Demographics> demographics,
        int facesAnalyzed,
        Map<String, Object> attributes
    ) {
    }

    /**
     * Demographic information for a single face.
     *
     * @param age estimated age (years)
     * @param ageRange estimated age range (e.g., "25-30")
     * @param gender estimated gender (e.g., "Male", "Female")
     * @param genderConfidence confidence score for gender prediction (0.0 to 1.0)
     * @param ageError estimated mean absolute error for age prediction
     * @param faceIndex index of the face this demographic applies to
     */
    record Demographics(
        int age,
        String ageRange,
        String gender,
        double genderConfidence,
        double ageError,
        int faceIndex
    ) {
    }
}

