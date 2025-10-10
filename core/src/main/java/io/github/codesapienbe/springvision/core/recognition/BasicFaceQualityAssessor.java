package io.github.codesapienbe.springvision.core.recognition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.codesapienbe.springvision.core.Detection;

/**
 * Basic implementation of face quality assessment for recognition.
 *
 * <p>This assessor evaluates face quality based on detection confidence,
 * bounding box size, and basic geometric properties. It's designed to
 * filter out low-quality faces that would likely result in poor
 * recognition accuracy.</p>
 *
 * <p>Quality factors considered:</p>
 * <ul>
 *   <li>Detection confidence score</li>
 *   <li>Face size relative to image</li>
 *   <li>Bounding box aspect ratio</li>
 *   <li>Face position in image</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
public class BasicFaceQualityAssessor implements FaceQualityAssessor {

    private static final Logger logger = LoggerFactory.getLogger(BasicFaceQualityAssessor.class);

    // Quality thresholds
    private static final double MIN_CONFIDENCE = 0.5;
    private static final double MIN_FACE_SIZE_RATIO = 0.01; // 1% of image area
    private static final double MAX_FACE_SIZE_RATIO = 0.8;  // 80% of image area
    private static final double MIN_ASPECT_RATIO = 0.5;     // Width/Height ratio
    private static final double MAX_ASPECT_RATIO = 2.0;
    private static final double EDGE_MARGIN = 0.1;          // 10% margin from edges

    @Override
    public double assessQuality(Detection detection) {
        if (detection == null) {
            return 0.0;
        }

        try {
            // Start with detection confidence
            double qualityScore = detection.confidence();

            // Apply quality factors
            qualityScore *= assessBoundingBoxQuality(detection);
            qualityScore *= assessPositionQuality(detection);

            // Ensure score is within valid range
            qualityScore = Math.max(0.0, Math.min(1.0, qualityScore));

            logger.debug("Face quality assessment: confidence={:.3f}, final_score={:.3f}",
                detection.confidence(), qualityScore);

            return qualityScore;

        } catch (Exception e) {
            logger.warn("Error assessing face quality: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Assess quality based on bounding box properties.
     */
    private double assessBoundingBoxQuality(Detection detection) {
        var bbox = detection.boundingBox();
        if (bbox == null) {
            return 0.0;
        }

        double width = bbox.width();
        double height = bbox.height();

        // Check face size
        double faceArea = width * height;
        double imageArea = 1.0; // Assuming normalized coordinates (0-1)
        double faceSizeRatio = faceArea / imageArea;

        if (faceSizeRatio < MIN_FACE_SIZE_RATIO || faceSizeRatio > MAX_FACE_SIZE_RATIO) {
            logger.debug("Face size ratio {} outside acceptable range [{}, {}]",
                faceSizeRatio, MIN_FACE_SIZE_RATIO, MAX_FACE_SIZE_RATIO);
            return 0.3; // Penalize but don't completely reject
        }

        // Check aspect ratio
        double aspectRatio = width / height;
        if (aspectRatio < MIN_ASPECT_RATIO || aspectRatio > MAX_ASPECT_RATIO) {
            logger.debug("Face aspect ratio {} outside acceptable range [{}, {}]",
                aspectRatio, MIN_ASPECT_RATIO, MAX_ASPECT_RATIO);
            return 0.5; // Moderate penalty
        }

        // Size quality score (prefer medium-sized faces)
        double sizeQuality = 1.0;
        if (faceSizeRatio < 0.05) {
            sizeQuality = 0.7; // Small faces
        } else if (faceSizeRatio > 0.5) {
            sizeQuality = 0.8; // Very large faces
        }

        return sizeQuality;
    }

    /**
     * Assess quality based on face position in image.
     */
    private double assessPositionQuality(Detection detection) {
        var bbox = detection.boundingBox();
        if (bbox == null) {
            return 0.0;
        }

        double x = bbox.x();
        double y = bbox.y();
        double width = bbox.width();
        double height = bbox.height();

        // Check if face is too close to image edges
        double leftMargin = x;
        double rightMargin = 1.0 - (x + width);
        double topMargin = y;
        double bottomMargin = 1.0 - (y + height);

        double minMargin = Math.min(Math.min(leftMargin, rightMargin),
            Math.min(topMargin, bottomMargin));

        if (minMargin < EDGE_MARGIN) {
            logger.debug("Face too close to edge: min_margin={:.3f} < {:.3f}",
                minMargin, EDGE_MARGIN);
            return 0.6; // Penalty for edge faces
        }

        // Position quality score (prefer centered faces)
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        double distanceFromCenter = Math.sqrt(
            Math.pow(centerX - 0.5, 2) + Math.pow(centerY - 0.5, 2)
        );

        // Quality decreases with distance from center
        double positionQuality = Math.max(0.5, 1.0 - distanceFromCenter);

        return positionQuality;
    }

    /**
     * Get a detailed quality assessment with individual factors.
     * @param detection The detection to assess.
     * @return A detailed quality assessment.
     */
    public QualityAssessment getDetailedAssessment(Detection detection) {
        if (detection == null) {
            return new QualityAssessment(0.0, "No detection provided");
        }

        try {
            double confidence = detection.confidence();
            double bboxQuality = assessBoundingBoxQuality(detection);
            double positionQuality = assessPositionQuality(detection);
            double overallQuality = confidence * bboxQuality * positionQuality;

            String reason = buildQualityReason(confidence, bboxQuality, positionQuality);

            return new QualityAssessment(overallQuality, reason);

        } catch (Exception e) {
            logger.warn("Error in detailed quality assessment: {}", e.getMessage());
            return new QualityAssessment(0.0, "Assessment error: " + e.getMessage());
        }
    }

    /**
     * Build a human-readable reason for the quality score.
     */
    private String buildQualityReason(double confidence, double bboxQuality, double positionQuality) {
        StringBuilder reason = new StringBuilder();

        if (confidence < MIN_CONFIDENCE) {
            reason.append("Low detection confidence (").append(String.format("%.2f", confidence)).append(")");
        }

        if (bboxQuality < 0.8) {
            if (reason.length() > 0) reason.append(", ");
            reason.append("Poor bounding box quality (").append(String.format("%.2f", bboxQuality)).append(")");
        }

        if (positionQuality < 0.8) {
            if (reason.length() > 0) reason.append(", ");
            reason.append("Poor position quality (").append(String.format("%.2f", positionQuality)).append(")");
        }

        if (reason.length() == 0) {
            reason.append("Good quality face");
        }

        return reason.toString();
    }

    /**
     * Check if a face meets minimum quality requirements for recognition.
     * @param detection The detection to check.
     * @return {@code true} if the face is ready for recognition, {@code false} otherwise.
     */
    public boolean isRecognitionReady(Detection detection) {
        return assessQuality(detection) >= MIN_CONFIDENCE;
    }

    /**
     * Get the minimum confidence threshold for face detection.
     * @return The minimum confidence threshold.
     */
    public double getMinConfidence() {
        return MIN_CONFIDENCE;
    }

    /**
     * Get the minimum face size ratio threshold.
     * @return The minimum face size ratio.
     */
    public double getMinFaceSizeRatio() {
        return MIN_FACE_SIZE_RATIO;
    }

    /**
     * Get the maximum face size ratio threshold.
     * @return The maximum face size ratio.
     */
    public double getMaxFaceSizeRatio() {
        return MAX_FACE_SIZE_RATIO;
    }
}
