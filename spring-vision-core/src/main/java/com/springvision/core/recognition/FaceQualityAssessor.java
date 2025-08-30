package com.springvision.core.recognition;

import com.springvision.core.Detection;

/**
 * Interface for assessing face quality for recognition purposes.
 * 
 * <p>Face quality assessment is crucial for face recognition accuracy.
 * This interface provides methods to evaluate the quality of detected
 * faces and determine if they are suitable for recognition.</p>
 * 
 * <p>Quality factors typically include:</p>
 * <ul>
 *   <li>Detection confidence</li>
 *   <li>Face size and resolution</li>
 *   <li>Image blur and sharpness</li>
 *   <li>Lighting conditions</li>
 *   <li>Face pose and orientation</li>
 *   <li>Occlusion and obstructions</li>
 * </ul>
 * 
 * @author Spring Vision Team
 * @since 1.0.0
 */
public interface FaceQualityAssessor {

    /**
     * Assess the quality of a detected face.
     * 
     * @param detection the face detection to assess
     * @return quality score between 0.0 (poor) and 1.0 (excellent)
     */
    double assessQuality(Detection detection);
} 