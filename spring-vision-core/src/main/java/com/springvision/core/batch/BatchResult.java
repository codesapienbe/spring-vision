package com.springvision.core.batch;

import com.springvision.core.DetectionType;
import com.springvision.core.VisionResult;
import java.util.List;

/**
 * Represents the result of processing a batch with a specific detection type.
 *
 * <p>This class encapsulates the results of processing a batch of images with a particular
 * detection type, providing a clean separation between different detection operations
 * in multi-type batch processing.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see BatchVisionProcessor
 * @see VisionResult
 */
public final class BatchResult {

    private final DetectionType detectionType;
    private final List<VisionResult> results;

    /**
     * Creates a new BatchResult instance.
     *
     * @param detectionType the detection type that was used
     * @param results the vision results
     */
    public BatchResult(DetectionType detectionType, List<VisionResult> results) {
        this.detectionType = detectionType;
        this.results = List.copyOf(results); // Defensive copy
    }

    /**
     * Gets the detection type that was used for this batch.
     *
     * @return the detection type
     */
    public DetectionType getDetectionType() {
        return detectionType;
    }

    /**
     * Gets the vision results for this batch.
     *
     * @return the vision results
     */
    public List<VisionResult> getResults() {
        return results;
    }

    /**
     * Gets the number of results in this batch.
     *
     * @return the number of results
     */
    public int getResultCount() {
        return results.size();
    }

    /**
     * Checks if this batch has any results.
     *
     * @return true if there are results, false otherwise
     */
    public boolean hasResults() {
        return !results.isEmpty();
    }

        /**
     * Gets the number of successful detections in this batch.
     *
     * @return the number of successful detections
     */
    public long getSuccessfulDetections() {
        return results.stream()
            .filter(result -> result.detections() != null && !result.detections().isEmpty())
            .count();
    }

    /**
     * Gets the average confidence score for this batch.
     *
     * @return the average confidence score, or 0.0 if no results
     */
    public double getAverageConfidence() {
        if (results.isEmpty()) {
            return 0.0;
        }

        return results.stream()
            .mapToDouble(VisionResult::averageConfidence)
            .average()
            .orElse(0.0);
    }

    @Override
    public String toString() {
        return String.format("BatchResult{detectionType=%s, resultCount=%d, successfulDetections=%d, avgConfidence=%.2f}",
                           detectionType, results.size(), getSuccessfulDetections(), getAverageConfidence());
    }
}
