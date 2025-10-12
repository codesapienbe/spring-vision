package io.github.codesapienbe.springvision.starter.web.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.codesapienbe.springvision.core.Detection;

/**
 * Response DTO for detection operations.
 *
 * <p>This DTO represents the response from detection operations, including
 * detection results, metadata, and error information if applicable.</p>
 *
 * <p>Example response:</p>
 * <pre>{@code
 * {
 *   "correlationId": "123e4567-e89b-12d3-a456-426614174000",
 *   "detectionType": "face",
 *   "detectionCount": 2,
 *   "averageConfidence": 0.85,
 *   "processingTimeMs": 150,
 *   "detections": [
 *     {
 *       "label": "face",
 *       "confidence": 0.92,
 *       "boundingBox": {"x": 0.1, "y": 0.2, "width": 0.3, "height": 0.4}
 *     }
 *   ]
 * }
 * }</pre>
 *
 * @author Spring Vision Team
 * @see DetectionRequest
 * @see Detection
 * @since 1.0.0
 */
public class DetectionResponse {

    /**
     * Unique correlation ID for request tracking.
     */
    @JsonProperty("correlationId")
    private String correlationId;

    /**
     * The type of detection performed.
     */
    @JsonProperty("detectionType")
    private String detectionType;

    /**
     * Number of detections found.
     */
    @JsonProperty("detectionCount")
    private int detectionCount;

    /**
     * Average confidence score of all detections.
     */
    @JsonProperty("averageConfidence")
    private double averageConfidence;

    /**
     * Processing time in milliseconds.
     */
    @JsonProperty("processingTimeMs")
    private long processingTimeMs;

    /**
     * List of individual detections.
     */
    @JsonProperty("detections")
    private List<Detection> detections;

    /**
     * Error message if the operation failed.
     */
    @JsonProperty("error")
    private String error;

    /**
     * Default constructor for JSON deserialization.
     */
    public DetectionResponse() {
    }

    /**
     * Constructs a new detection response.
     *
     * @param correlationId     the correlation ID
     * @param detectionType     the detection type
     * @param detectionCount    the detection count
     * @param averageConfidence the average confidence
     * @param processingTimeMs  the processing time
     * @param detections        the list of detections
     * @param error             the error message
     */
    public DetectionResponse(String correlationId, String detectionType, int detectionCount,
                             double averageConfidence, long processingTimeMs, List<Detection> detections, String error) {
        this.correlationId = correlationId;
        this.detectionType = detectionType;
        this.detectionCount = detectionCount;
        this.averageConfidence = averageConfidence;
        this.processingTimeMs = processingTimeMs;
        this.detections = detections;
        this.error = error;
    }

    /**
     * Gets the correlation ID.
     *
     * @return the correlation ID
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Sets the correlation ID.
     *
     * @param correlationId the correlation ID
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * Gets the detection type.
     *
     * @return the detection type
     */
    public String getDetectionType() {
        return detectionType;
    }

    /**
     * Sets the detection type.
     *
     * @param detectionType the detection type
     */
    public void setDetectionType(String detectionType) {
        this.detectionType = detectionType;
    }

    /**
     * Gets the detection count.
     *
     * @return the detection count
     */
    public int getDetectionCount() {
        return detectionCount;
    }

    /**
     * Sets the detection count.
     *
     * @param detectionCount the detection count
     */
    public void setDetectionCount(int detectionCount) {
        this.detectionCount = detectionCount;
    }

    /**
     * Gets the average confidence.
     *
     * @return the average confidence
     */
    public double getAverageConfidence() {
        return averageConfidence;
    }

    /**
     * Sets the average confidence.
     *
     * @param averageConfidence the average confidence
     */
    public void setAverageConfidence(double averageConfidence) {
        this.averageConfidence = averageConfidence;
    }

    /**
     * Gets the processing time in milliseconds.
     *
     * @return the processing time
     */
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    /**
     * Sets the processing time in milliseconds.
     *
     * @param processingTimeMs the processing time
     */
    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    /**
     * Gets the list of detections.
     *
     * @return the list of detections
     */
    public List<Detection> getDetections() {
        return detections;
    }

    /**
     * Sets the list of detections.
     *
     * @param detections the list of detections
     */
    public void setDetections(List<Detection> detections) {
        this.detections = detections;
    }

    /**
     * Gets the error message.
     *
     * @return the error message
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error message.
     *
     * @param error the error message
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Checks if the response contains an error.
     *
     * @return true if there is an error, false otherwise
     */
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    /**
     * Checks if the response contains detections.
     *
     * @return true if there are detections, false otherwise
     */
    public boolean hasDetections() {
        return detections != null && !detections.isEmpty();
    }

    /**
     * Builder for creating DetectionResponse instances.
     */
    public static class Builder {
        private String correlationId;
        private String detectionType;
        private int detectionCount;
        private double averageConfidence;
        private long processingTimeMs;
        private List<Detection> detections;
        private String error;

        /**
         * Default constructor for the builder.
         */
        public Builder() {
        }

        /**
         * Sets the correlation ID.
         *
         * @param correlationId the correlation ID
         * @return this builder
         */
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        /**
         * Sets the detection type.
         *
         * @param detectionType the detection type
         * @return this builder
         */
        public Builder detectionType(String detectionType) {
            this.detectionType = detectionType;
            return this;
        }

        /**
         * Sets the detection count.
         *
         * @param detectionCount the detection count
         * @return this builder
         */
        public Builder detectionCount(int detectionCount) {
            this.detectionCount = detectionCount;
            return this;
        }

        /**
         * Sets the average confidence.
         *
         * @param averageConfidence the average confidence
         * @return this builder
         */
        public Builder averageConfidence(double averageConfidence) {
            this.averageConfidence = averageConfidence;
            return this;
        }

        /**
         * Sets the processing time in milliseconds.
         *
         * @param processingTimeMs the processing time
         * @return this builder
         */
        public Builder processingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        /**
         * Sets the list of detections.
         *
         * @param detections the list of detections
         * @return this builder
         */
        public Builder detections(List<Detection> detections) {
            this.detections = detections;
            return this;
        }

        /**
         * Sets the error message.
         *
         * @param error the error message
         * @return this builder
         */
        public Builder error(String error) {
            this.error = error;
            return this;
        }

        /**
         * Builds a new DetectionResponse.
         *
         * @return the built DetectionResponse
         */
        public DetectionResponse build() {
            return new DetectionResponse(correlationId, detectionType, detectionCount,
                averageConfidence, processingTimeMs, detections, error);
        }
    }

    /**
     * Creates a new builder for DetectionResponse.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
