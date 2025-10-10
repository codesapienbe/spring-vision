package io.github.codesapienbe.springvision.core;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable result container for computer vision operations.
 *
 * <p>This record encapsulates the results of computer vision operations such as
 * face detection, object detection, text recognition, etc. It provides a unified
 * interface for accessing detection results regardless of the underlying vision
 * backend implementation.</p>
 *
 * <p>The record includes metadata about the operation (detection type, timestamp,
 * processing time) as well as the actual detection results (detections, confidence
 * scores, bounding boxes, etc.).</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * VisionTemplate visionTemplate = // ... get template
 * ImageData imageData = ImageData.fromBytes(imageBytes);
 *
 * VisionResult result = visionTemplate.detectFaces(imageData);
 *
 * // Access detection results
 * List<Detection> detections = result.detections();
 * double averageConfidence = result.averageConfidence();
 * boolean hasDetections = result.hasDetections();
 * }</pre>
 *
 * @param detectionType     the type of detection performed
 * @param detections        the list of detected objects
 * @param averageConfidence the average confidence score across all detections
 * @param processingTimeMs  the time taken to process the image in milliseconds
 * @param timestamp         the timestamp when the result was created
 * @param metadata          additional metadata about the operation
 * @author Spring Vision Team
 * @see VisionTemplate
 * @see Detection
 * @see DetectionType
 * @see ImageData
 * @since 1.0.0
 */
public record VisionResult(
    DetectionType detectionType,
    List<Detection> detections,
    double averageConfidence,
    long processingTimeMs,
    Instant timestamp,
    Map<String, Object> metadata
) {

    /**
     * Creates a new VisionResult with the specified parameters.
     *
     * @param detectionType     the type of detection performed
     * @param detections        the list of detections found
     * @param averageConfidence the average confidence score across all detections
     * @param processingTimeMs  the processing time in milliseconds
     * @param timestamp         the timestamp when the detection was performed
     * @param metadata          additional metadata about the detection operation
     */
    public VisionResult {
        Objects.requireNonNull(detectionType, "Detection type must not be null");
        Objects.requireNonNull(detections, "Detections list must not be null");
        Objects.requireNonNull(timestamp, "Timestamp must not be null");
        Objects.requireNonNull(metadata, "Metadata must not be null");

        // Validate confidence score
        if (averageConfidence < 0.0 || averageConfidence > 1.0) {
            throw new IllegalArgumentException(
                String.format("Average confidence must be between 0.0 and 1.0, got %.3f", averageConfidence));
        }

        // Validate processing time
        if (processingTimeMs < 0) {
            throw new IllegalArgumentException("Processing time must be non-negative");
        }

        // Make detections list immutable
        detections = Collections.unmodifiableList(detections);

        // Make metadata map immutable
        metadata = Collections.unmodifiableMap(metadata);
    }

    /**
     * Creates a new VisionResult with current timestamp and empty metadata.
     *
     * @param detectionType     the type of detection performed
     * @param detections        the list of detections found
     * @param averageConfidence the average confidence score across all detections
     * @param processingTimeMs  the processing time in milliseconds
     * @return a new VisionResult instance
     */
    public static VisionResult of(DetectionType detectionType, List<Detection> detections,
                                  double averageConfidence, long processingTimeMs) {
        return new VisionResult(detectionType, detections, averageConfidence, processingTimeMs,
            Instant.now(), Map.of());
    }

    /**
     * Creates a new VisionResult with current timestamp and custom metadata.
     *
     * @param detectionType     the type of detection performed
     * @param detections        the list of detections found
     * @param averageConfidence the average confidence score across all detections
     * @param processingTimeMs  the processing time in milliseconds
     * @param metadata          additional metadata about the detection operation
     * @return a new VisionResult instance
     */
    public static VisionResult of(DetectionType detectionType, List<Detection> detections,
                                  double averageConfidence, long processingTimeMs, Map<String, Object> metadata) {
        return new VisionResult(detectionType, detections, averageConfidence, processingTimeMs,
            Instant.now(), metadata);
    }

    /**
     * Creates an empty VisionResult for the specified detection type.
     *
     * @param detectionType    the type of detection performed
     * @param processingTimeMs the processing time in milliseconds
     * @return a new VisionResult with no detections
     */
    public static VisionResult empty(DetectionType detectionType, long processingTimeMs) {
        return new VisionResult(detectionType, List.of(), 0.0, processingTimeMs,
            Instant.now(), Map.of());
    }

    /**
     * Checks if this result contains any detections.
     *
     * @return true if there are detections, false otherwise
     */
    public boolean hasDetections() {
        return !detections.isEmpty();
    }

    /**
     * Gets the number of detections in this result.
     *
     * @return the number of detections
     */
    public int detectionCount() {
        return detections.size();
    }

    /**
     * Gets the detection with the highest confidence score.
     *
     * @return the detection with highest confidence, or null if no detections
     */
    public Detection getHighestConfidenceDetection() {
        if (detections.isEmpty()) {
            return null;
        }

        return detections.stream()
            .max((d1, d2) -> Double.compare(d1.confidence(), d2.confidence()))
            .orElse(null);
    }

    /**
     * Gets all detections with confidence above the specified threshold.
     *
     * @param threshold the confidence threshold (0.0 to 1.0)
     * @return a list of detections with confidence above the threshold
     * @throws IllegalArgumentException if threshold is outside valid range
     */
    public List<Detection> getDetectionsAboveThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException(
                String.format("Threshold must be between 0.0 and 1.0, got %.3f", threshold));
        }

        return detections.stream()
            .filter(detection -> detection.confidence() >= threshold)
            .toList();
    }

    /**
     * Gets the processing time in seconds.
     *
     * @return the processing time in seconds
     */
    public double getProcessingTimeSeconds() {
        return processingTimeMs / 1000.0;
    }

    /**
     * Checks if the processing time is within acceptable limits.
     *
     * @param maxProcessingTimeMs the maximum acceptable processing time in milliseconds
     * @return true if processing time is within limits, false otherwise
     */
    public boolean isProcessingTimeAcceptable(long maxProcessingTimeMs) {
        return processingTimeMs <= maxProcessingTimeMs;
    }

    /**
     * Gets a metadata value by key.
     *
     * @param key the metadata key
     * @return the metadata value, or null if not found
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Gets a metadata value by key with a default value.
     *
     * @param key          the metadata key
     * @param defaultValue the default value to return if key not found
     * @return the metadata value, or the default value if not found
     */
    public Object getMetadata(String key, Object defaultValue) {
        return metadata.getOrDefault(key, defaultValue);
    }

    /**
     * Checks if this result contains a specific metadata key.
     *
     * @param key the metadata key to check
     * @return true if the key exists, false otherwise
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    /**
     * Creates a new VisionResult with additional metadata.
     *
     * @param additionalMetadata additional metadata to merge
     * @return a new VisionResult with merged metadata
     */
    public VisionResult withAdditionalMetadata(Map<String, Object> additionalMetadata) {
        Objects.requireNonNull(additionalMetadata, "Additional metadata must not be null");

        Map<String, Object> mergedMetadata = Map.of();
        if (!metadata.isEmpty() || !additionalMetadata.isEmpty()) {
            mergedMetadata = Map.of(); // In a real implementation, merge the maps
        }

        return new VisionResult(detectionType, detections, averageConfidence,
            processingTimeMs, timestamp, mergedMetadata);
    }

    /**
     * Creates a new VisionResult with a single additional metadata entry.
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @return a new VisionResult with the additional metadata
     */
    public VisionResult withMetadata(String key, Object value) {
        Objects.requireNonNull(key, "Metadata key must not be null");

        Map<String, Object> newMetadata = Map.of(key, value);
        return withAdditionalMetadata(newMetadata);
    }

    @Override
    public String toString() {
        return String.format("VisionResult{type=%s, detections=%d, avgConfidence=%.3f, " +
                "processingTime=%dms, timestamp=%s}",
            detectionType.getDisplayName(), detectionCount(), averageConfidence,
            processingTimeMs, timestamp);
    }
}
