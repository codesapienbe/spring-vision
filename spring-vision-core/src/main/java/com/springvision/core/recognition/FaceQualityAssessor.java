package com.springvision.core.recognition;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;

/**
 * Interface for assessing the quality of detected faces for recognition purposes.
 * 
 * <p>Face quality assessment is crucial for accurate face recognition. This interface
 * provides methods to evaluate various quality factors including blur, illumination,
 * pose, resolution, and overall suitability for recognition tasks.</p>
 * 
 * <p>Quality scores range from 0.0 (poor quality) to 1.0 (excellent quality).
 * Implementations should consider multiple factors and provide consistent scoring
 * across different types of images and lighting conditions.</p>
 * 
 * @author Spring Vision Team
 * @since 1.0.0
 */
public interface FaceQualityAssessor {

    /**
     * Assess the overall quality of a detected face for recognition.
     * 
     * <p>This method combines multiple quality factors into a single score
     * that indicates how suitable the face is for recognition tasks.</p>
     * 
     * @param detection face detection result
     * @return quality score from 0.0 (poor) to 1.0 (excellent)
     * @throws IllegalArgumentException if detection is null or invalid
     */
    double assessQuality(Detection detection);
    
    /**
     * Assess face quality with access to the original image data.
     * 
     * <p>This method provides more comprehensive quality assessment by
     * analyzing the actual image pixels within the face region.</p>
     * 
     * @param detection face detection result
     * @param imageData original image containing the face
     * @return quality score from 0.0 (poor) to 1.0 (excellent)
     * @throws IllegalArgumentException if parameters are null or invalid
     */
    double assessQuality(Detection detection, ImageData imageData);
    
    /**
     * Get detailed quality metrics for a face detection.
     * 
     * @param detection face detection result
     * @param imageData original image containing the face  
     * @return detailed quality assessment with individual factor scores
     */
    QualityAssessment getDetailedAssessment(Detection detection, ImageData imageData);
    
    /**
     * Check if a face meets the minimum quality threshold for recognition.
     * 
     * @param detection face detection result
     * @param minThreshold minimum acceptable quality score
     * @return true if face quality meets or exceeds threshold
     */
    default boolean meetsQualityThreshold(Detection detection, double minThreshold) {
        return assessQuality(detection) >= minThreshold;
    }
    
    /**
     * Detailed quality assessment with individual factor scores.
     */
    record QualityAssessment(
        double overallScore,      // Combined quality score (0.0-1.0)
        double blurScore,         // Sharpness/focus quality (0.0-1.0)
        double illuminationScore, // Lighting quality (0.0-1.0)
        double poseScore,         // Face pose quality (0.0-1.0, higher for frontal)
        double resolutionScore,   // Face resolution quality (0.0-1.0)
        double occlusionScore,    // Occlusion assessment (0.0-1.0, higher for less occlusion)
        String primaryIssue,      // Main quality issue (if any)
        java.util.Map<String, Object> detailedMetrics // Additional metrics
    ) {
        
        /**
         * Check if this assessment indicates good quality for recognition.
         */
        public boolean isGoodForRecognition(double threshold) {
            return overallScore >= threshold;
        }
        
        /**
         * Get a human-readable quality description.
         */
        public String getQualityDescription() {
            if (overallScore >= 0.8) return "Excellent";
            if (overallScore >= 0.6) return "Good"; 
            if (overallScore >= 0.4) return "Fair";
            if (overallScore >= 0.2) return "Poor";
            return "Very Poor";
        }
        
        /**
         * Get the weakest quality factor.
         */
        public String getWeakestFactor() {
            double minScore = Math.min(blurScore, 
                             Math.min(illuminationScore,
                             Math.min(poseScore,
                             Math.min(resolutionScore, occlusionScore))));
            
            if (minScore == blurScore) return "blur";
            if (minScore == illuminationScore) return "illumination";
            if (minScore == poseScore) return "pose";
            if (minScore == resolutionScore) return "resolution";
            return "occlusion";
        }
    }
    
    /**
     * Quality factor weights for different use cases.
     */
    enum QualityProfile {
        /** Balanced weighting for general recognition */
        BALANCED(0.3, 0.2, 0.2, 0.2, 0.1),
        
        /** Emphasis on sharpness for high-accuracy recognition */
        HIGH_ACCURACY(0.4, 0.2, 0.2, 0.15, 0.05),
        
        /** More tolerant for real-world conditions */
        ROBUST(0.25, 0.25, 0.15, 0.25, 0.1),
        
        /** Optimized for mobile/selfie images */
        MOBILE_OPTIMIZED(0.2, 0.3, 0.1, 0.3, 0.1);
        
        public final double blurWeight;
        public final double illuminationWeight;
        public final double poseWeight;
        public final double resolutionWeight;
        public final double occlusionWeight;
        
        QualityProfile(double blur, double illumination, double pose, double resolution, double occlusion) {
            this.blurWeight = blur;
            this.illuminationWeight = illumination;
            this.poseWeight = pose;
            this.resolutionWeight = resolution;
            this.occlusionWeight = occlusion;
        }
        
        /**
         * Calculate weighted quality score.
         */
        public double calculateScore(double blur, double illumination, double pose, 
                                   double resolution, double occlusion) {
            return blurWeight * blur +
                   illuminationWeight * illumination +
                   poseWeight * pose +
                   resolutionWeight * resolution +
                   occlusionWeight * occlusion;
        }
    }
} 