package io.github.codesapienbe.springvision.core.health;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a stress analysis sample containing stress metrics and metadata.
 *
 * @param timestamp   The time when this sample was recorded
 * @param stressLevel The calculated stress level (0.0 to 1.0)
 * @param confidence  The confidence of the stress detection (0.0 to 1.0)
 * @param metrics     Additional stress metrics and biometric data
 * @param source      The source of the stress analysis (e.g., "facial_analysis", "heart_rate")
 * @author Spring Vision Team
 * @since 1.1.0
 */
public record StressSample(
    LocalDateTime timestamp,
    double stressLevel,
    double confidence,
    Map<String, Object> metrics,
    String source
) {

    /**
     * Minimum valid stress level (0.0).
     */
    public static final double MIN_STRESS_LEVEL = 0.0;

    /**
     * Maximum valid stress level (1.0).
     */
    public static final double MAX_STRESS_LEVEL = 1.0;

    /**
     * Creates a StressSample with validation.
     */
    public StressSample {
        if (stressLevel < MIN_STRESS_LEVEL || stressLevel > MAX_STRESS_LEVEL) {
            throw new IllegalArgumentException("Stress level must be between 0.0 and 1.0");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Source cannot be null or empty");
        }
    }

    /**
     * Creates a basic stress sample with the current timestamp.
     */
    public static StressSample of(double stressLevel, double confidence, String source) {
        return new StressSample(LocalDateTime.now(), stressLevel, confidence, Map.of(), source);
    }

    /**
     * Creates a stress sample with additional metrics.
     */
    public static StressSample withMetrics(double stressLevel, double confidence, String source, Map<String, Object> metrics) {
        return new StressSample(LocalDateTime.now(), stressLevel, confidence, metrics, source);
    }
}
