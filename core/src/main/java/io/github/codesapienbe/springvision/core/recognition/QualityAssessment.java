package io.github.codesapienbe.springvision.core.recognition;

/**
 * Represents a detailed quality assessment for a face detection.
 *
 * <p>This class provides comprehensive information about the quality
 * of a detected face, including the overall quality score and a
 * human-readable reason for the assessment.</p>
 *
 * @param qualityScore the overall quality score (0.0-1.0)
 * @param reason       the human-readable reason for the score
 * @author Spring Vision Team
 * @since 1.0.0
 */
public record QualityAssessment(
    double qualityScore,  // Overall quality score (0.0-1.0)
    String reason        // Human-readable reason for the score
) {

    /**
     * Create a quality assessment.
     *
     * @param qualityScore overall quality score between 0.0 and 1.0
     * @param reason       human-readable explanation of the quality assessment
     */
    public QualityAssessment {
        if (qualityScore < 0.0 || qualityScore > 1.0) {
            throw new IllegalArgumentException("Quality score must be between 0.0 and 1.0");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason must not be null or empty");
        }
    }

    /**
     * Check if this assessment indicates good quality for recognition.
     *
     * @param threshold minimum acceptable quality score
     * @return true if quality meets or exceeds threshold
     */
    public boolean isGoodForRecognition(double threshold) {
        return qualityScore >= threshold;
    }

    /**
     * Get a human-readable quality description.
     *
     * @return quality level description
     */
    public String getQualityDescription() {
        if (qualityScore >= 0.8) return "Excellent";
        if (qualityScore >= 0.6) return "Good";
        if (qualityScore >= 0.4) return "Fair";
        if (qualityScore >= 0.2) return "Poor";
        return "Very Poor";
    }

    /**
     * Check if this face is suitable for recognition with default threshold.
     *
     * @return true if quality is good enough for recognition
     */
    public boolean isRecognitionReady() {
        return isGoodForRecognition(0.5); // Default threshold
    }

    @Override
    public String toString() {
        return String.format("QualityAssessment{score=%.3f, description='%s', reason='%s'}",
            qualityScore, getQualityDescription(), reason);
    }
}
