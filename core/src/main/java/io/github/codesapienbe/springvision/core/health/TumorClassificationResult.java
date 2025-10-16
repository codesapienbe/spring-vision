package io.github.codesapienbe.springvision.core.health;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of a brain tumor classification analysis.
 *
 * @param tumorDetected     Whether a tumor was detected in the MRI image
 * @param tumorType         The type of tumor detected (if any)
 * @param confidence        The confidence level of the classification (0.0 to 1.0)
 * @param regions           List of detected tumor regions with coordinates
 * @param metadata          Additional classification metadata and metrics
 * @param analysisTimestamp When the analysis was performed
 * @author Spring Vision Team
 * @since 1.1.0
 */
public record TumorClassificationResult(
    boolean tumorDetected,
    String tumorType,
    double confidence,
    List<TumorRegion> regions,
    Map<String, Object> metadata,
    LocalDateTime analysisTimestamp
) {

    /**
     * Creates a TumorClassificationResult with validation.
     */
    public TumorClassificationResult {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
        if (analysisTimestamp == null) {
            throw new IllegalArgumentException("Analysis timestamp cannot be null");
        }
        if (tumorDetected && (tumorType == null || tumorType.trim().isEmpty())) {
            throw new IllegalArgumentException("Tumor type must be specified when tumor is detected");
        }
        if (regions == null) {
            throw new IllegalArgumentException("Regions list cannot be null");
        }
    }

    /**
     * Creates a result indicating no tumor was detected.
     */
    public static TumorClassificationResult noTumorDetected(double confidence) {
        return new TumorClassificationResult(
            false,
            null,
            confidence,
            List.of(),
            Map.of(),
            LocalDateTime.now()
        );
    }

    /**
     * Creates a result indicating a tumor was detected.
     */
    public static TumorClassificationResult tumorDetected(String tumorType, double confidence,
                                                          List<TumorRegion> regions) {
        return new TumorClassificationResult(
            true,
            tumorType,
            confidence,
            regions,
            Map.of(),
            LocalDateTime.now()
        );
    }

    /**
     * Creates a result with additional metadata.
     */
    public static TumorClassificationResult withMetadata(boolean tumorDetected, String tumorType,
                                                         double confidence, List<TumorRegion> regions,
                                                         Map<String, Object> metadata) {
        return new TumorClassificationResult(
            tumorDetected,
            tumorType,
            confidence,
            regions,
            metadata,
            LocalDateTime.now()
        );
    }

    /**
     * Represents a tumor region within an MRI image.
     */
    public record TumorRegion(
        double x,
        double y,
        double width,
        double height,
        double confidence,
        String subtype
    ) {
        public TumorRegion {
            if (confidence < 0.0 || confidence > 1.0) {
                throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
            }
            if (x < 0.0 || y < 0.0 || width <= 0.0 || height <= 0.0) {
                throw new IllegalArgumentException("Region coordinates must be positive");
            }
        }
    }
}
