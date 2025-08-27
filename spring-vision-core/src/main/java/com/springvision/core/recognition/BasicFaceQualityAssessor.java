package com.springvision.core.recognition;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;

/**
 * Basic implementation of FaceQualityAssessor.
 * 
 * <p>This implementation provides face quality assessment based on detection
 * attributes and basic heuristics. It combines multiple factors including
 * face size, confidence, and available quality metrics from the detection.</p>
 * 
 * <p>For more advanced quality assessment that analyzes the actual image pixels,
 * consider implementing a custom FaceQualityAssessor that uses computer vision
 * algorithms for blur detection, pose estimation, etc.</p>
 * 
 * @author Spring Vision Team
 * @since 1.0.0
 */
public class BasicFaceQualityAssessor implements FaceQualityAssessor {

    private static final Logger logger = LoggerFactory.getLogger(BasicFaceQualityAssessor.class);

    private final QualityProfile qualityProfile;
    
    /**
     * Create a basic face quality assessor with default balanced profile.
     */
    public BasicFaceQualityAssessor() {
        this(QualityProfile.BALANCED);
    }
    
    /**
     * Create a basic face quality assessor with specified quality profile.
     * 
     * @param qualityProfile weighting profile for different quality factors
     */
    public BasicFaceQualityAssessor(QualityProfile qualityProfile) {
        this.qualityProfile = qualityProfile;
        logger.info("Basic face quality assessor initialized with {} profile", qualityProfile);
    }

    @Override
    public double assessQuality(Detection detection) {
        if (detection == null) {
            throw new IllegalArgumentException("Detection must not be null");
        }
        
        return assessQualityFromAttributes(detection);
    }

    @Override
    public double assessQuality(Detection detection, ImageData imageData) {
        if (detection == null) {
            throw new IllegalArgumentException("Detection must not be null");
        }
        if (imageData == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }
        
        // For the basic implementation, we primarily use detection attributes
        // Advanced implementations could analyze the actual image pixels here
        double baseScore = assessQualityFromAttributes(detection);
        
        // Apply minor boost for having image data available (future enhancement point)
        return Math.min(1.0, baseScore * 1.05);
    }

    @Override
    public QualityAssessment getDetailedAssessment(Detection detection, ImageData imageData) {
        if (detection == null) {
            throw new IllegalArgumentException("Detection must not be null");
        }
        
        // Extract individual quality scores
        double blurScore = extractBlurScore(detection);
        double illuminationScore = extractIlluminationScore(detection);
        double poseScore = extractPoseScore(detection);
        double resolutionScore = extractResolutionScore(detection);
        double occlusionScore = extractOcclusionScore(detection);
        
        // Calculate overall score using profile weights
        double overallScore = qualityProfile.calculateScore(
            blurScore, illuminationScore, poseScore, resolutionScore, occlusionScore
        );
        
        // Identify primary issue
        String primaryIssue = identifyPrimaryIssue(blurScore, illuminationScore, 
                                                 poseScore, resolutionScore, occlusionScore);
        
        // Collect detailed metrics
        Map<String, Object> detailedMetrics = new HashMap<>();
        detailedMetrics.put("detection_confidence", detection.confidence());
        detailedMetrics.put("face_area_ratio", calculateFaceAreaRatio(detection));
        detailedMetrics.put("aspect_ratio", calculateAspectRatio(detection));
        detailedMetrics.put("quality_profile", qualityProfile.name());
        
        // Add backend-specific metrics if available
        detection.attributes().forEach((key, value) -> {
            if (key.contains("quality") || key.contains("score")) {
                detailedMetrics.put("backend_" + key, value);
            }
        });
        
        return new QualityAssessment(
            overallScore,
            blurScore,
            illuminationScore,
            poseScore,
            resolutionScore,
            occlusionScore,
            primaryIssue,
            detailedMetrics
        );
    }
    
    /**
     * Assess quality based on detection attributes and heuristics.
     */
    private double assessQualityFromAttributes(Detection detection) {
        Map<String, Object> attributes = detection.attributes();
        
        // Base score from detection confidence
        double baseScore = detection.confidence();
        
        // Apply quality score if available from backend
        if (attributes.containsKey("quality_score")) {
            Object qualityObj = attributes.get("quality_score");
            if (qualityObj instanceof Number) {
                double backendQuality = ((Number) qualityObj).doubleValue();
                baseScore = (baseScore + backendQuality) / 2.0; // Average with backend score
            }
        }
        
        // Apply face size bonus
        double sizeBonus = calculateSizeBonus(detection);
        baseScore = Math.min(1.0, baseScore + sizeBonus);
        
        // Apply detector-specific adjustments
        String detector = (String) attributes.get("detector");
        if (detector != null) {
            baseScore = applyDetectorAdjustment(baseScore, detector);
        }
        
        // Apply landmark bonus if available
        if (attributes.containsKey("has_landmarks") && 
            Boolean.TRUE.equals(attributes.get("has_landmarks"))) {
            baseScore = Math.min(1.0, baseScore + 0.05); // Small bonus for landmarks
        }
        
        return Math.max(0.0, Math.min(1.0, baseScore));
    }
    
    /**
     * Extract blur score from detection attributes.
     */
    private double extractBlurScore(Detection detection) {
        Map<String, Object> attributes = detection.attributes();
        
        // Check for backend-provided blur score
        if (attributes.containsKey("blur_score")) {
            Object blurObj = attributes.get("blur_score");
            if (blurObj instanceof Number) {
                return ((Number) blurObj).doubleValue();
            }
        }
        
        // Estimate from face size (larger faces tend to be sharper)
        double faceAreaRatio = calculateFaceAreaRatio(detection);
        if (faceAreaRatio > 0.15) return 0.9; // Large face, likely sharp
        if (faceAreaRatio > 0.08) return 0.7; // Medium face
        if (faceAreaRatio > 0.03) return 0.5; // Small face, potentially blurry
        return 0.3; // Very small face, likely blurry
    }
    
    /**
     * Extract illumination score from detection attributes.
     */
    private double extractIlluminationScore(Detection detection) {
        Map<String, Object> attributes = detection.attributes();
        
        // Check for backend-provided illumination score
        if (attributes.containsKey("illumination_score")) {
            Object illumObj = attributes.get("illumination_score");
            if (illumObj instanceof Number) {
                return ((Number) illumObj).doubleValue();
            }
        }
        
        // Default moderate score (can't assess without image analysis)
        return 0.7;
    }
    
    /**
     * Extract pose score from detection attributes.
     */
    private double extractPoseScore(Detection detection) {
        Map<String, Object> attributes = detection.attributes();
        
        // Check for backend-provided pose score
        if (attributes.containsKey("pose_quality")) {
            Object poseObj = attributes.get("pose_quality");
            if (poseObj instanceof Number) {
                return ((Number) poseObj).doubleValue();
            }
        }
        
        // Estimate from aspect ratio (frontal faces are closer to square)
        double aspectRatio = calculateAspectRatio(detection);
        if (aspectRatio >= 0.8 && aspectRatio <= 1.2) return 0.9; // Good frontal
        if (aspectRatio >= 0.7 && aspectRatio <= 1.4) return 0.7; // Slightly off
        if (aspectRatio >= 0.6 && aspectRatio <= 1.6) return 0.5; // Profile-ish
        return 0.3; // Likely extreme pose
    }
    
    /**
     * Extract resolution score from detection attributes.
     */
    private double extractResolutionScore(Detection detection) {
        Map<String, Object> attributes = detection.attributes();
        
        // Check for backend-provided resolution score
        if (attributes.containsKey("resolution_score")) {
            Object resObj = attributes.get("resolution_score");
            if (resObj instanceof Number) {
                return ((Number) resObj).doubleValue();
            }
        }
        
        // Estimate from face area ratio
        double faceAreaRatio = calculateFaceAreaRatio(detection);
        if (faceAreaRatio > 0.2) return 1.0;  // Large face, high resolution
        if (faceAreaRatio > 0.1) return 0.8;  // Good resolution
        if (faceAreaRatio > 0.05) return 0.6; // Acceptable resolution
        if (faceAreaRatio > 0.02) return 0.4; // Low resolution
        return 0.2; // Very low resolution
    }
    
    /**
     * Extract occlusion score from detection attributes.
     */
    private double extractOcclusionScore(Detection detection) {
        Map<String, Object> attributes = detection.attributes();
        
        // Check for backend-provided occlusion score
        if (attributes.containsKey("occlusion_score")) {
            Object occlObj = attributes.get("occlusion_score");
            if (occlObj instanceof Number) {
                return ((Number) occlObj).doubleValue();
            }
        }
        
        // Assume minimal occlusion by default (can't assess without detailed analysis)
        return 0.8;
    }
    
    /**
     * Calculate face area as ratio of image area.
     */
    private double calculateFaceAreaRatio(Detection detection) {
        var bbox = detection.boundingBox();
        return bbox.width() * bbox.height();
    }
    
    /**
     * Calculate face aspect ratio.
     */
    private double calculateAspectRatio(Detection detection) {
        var bbox = detection.boundingBox();
        return bbox.width() / bbox.height();
    }
    
    /**
     * Calculate size bonus based on face area.
     */
    private double calculateSizeBonus(Detection detection) {
        double areaRatio = calculateFaceAreaRatio(detection);
        
        // Optimal face size is around 10-25% of image
        if (areaRatio >= 0.1 && areaRatio <= 0.25) {
            return 0.1; // 10% bonus for optimal size
        } else if (areaRatio >= 0.05 && areaRatio <= 0.5) {
            return 0.05; // 5% bonus for acceptable size
        } else {
            return 0.0; // No bonus for very small or very large faces
        }
    }
    
    /**
     * Apply detector-specific quality adjustments.
     */
    private double applyDetectorAdjustment(double baseScore, String detector) {
        switch (detector) {
            case "yunet":
                return Math.min(1.0, baseScore * 1.1); // 10% bonus for YuNet (high accuracy)
            case "dnn_ssd":
                return Math.min(1.0, baseScore * 1.05); // 5% bonus for DNN
            case "haar_cascade":
                return baseScore * 0.95; // 5% penalty for Haar (lower accuracy)
            default:
                return baseScore;
        }
    }
    
    /**
     * Identify the primary quality issue.
     */
    private String identifyPrimaryIssue(double blur, double illumination, double pose, 
                                       double resolution, double occlusion) {
        double minScore = Math.min(blur, Math.min(illumination, Math.min(pose, Math.min(resolution, occlusion))));
        
        if (minScore > 0.7) {
            return "none"; // Good overall quality
        }
        
        if (minScore == blur && blur < 0.5) {
            return "blur";
        } else if (minScore == illumination && illumination < 0.5) {
            return "poor_lighting";
        } else if (minScore == pose && pose < 0.5) {
            return "non_frontal_pose";
        } else if (minScore == resolution && resolution < 0.5) {
            return "low_resolution";
        } else if (minScore == occlusion && occlusion < 0.5) {
            return "occlusion";
        } else {
            return "general_quality";
        }
    }
} 