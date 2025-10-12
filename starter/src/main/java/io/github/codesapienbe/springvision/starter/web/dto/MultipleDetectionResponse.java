package io.github.codesapienbe.springvision.starter.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for multiple detection operations.
 *
 * <p>This DTO represents the response from multiple detection operations, including
 * results for each requested detection type, metadata, and error information if applicable.</p>
 *
 * <p>Example response:</p>
 * <pre>{@code
 * {
 *   "correlationId": "123e4567-e89b-12d3-a456-426614174000",
 *   "detectionTypes": ["face", "object"],
 *   "results": [
 *     {
 *       "correlationId": "123e4567-e89b-12d3-a456-426614174000",
 *       "detectionType": "face",
 *       "detectionCount": 2,
 *       "averageConfidence": 0.85,
 *       "processingTimeMs": 150,
 *       "detections": [...]
 *     },
 *     {
 *       "correlationId": "123e4567-e89b-12d3-a456-426614174000",
 *       "detectionType": "object",
 *       "detectionCount": 1,
 *       "averageConfidence": 0.92,
 *       "processingTimeMs": 200,
 *       "detections": [...]
 *     }
 *   ]
 * }
 * }</pre>
 *
 * @author Spring Vision Team
 * @see MultipleDetectionRequest
 * @see DetectionResponse
 * @since 1.0.0
 */
public class MultipleDetectionResponse {

    /**
     * Unique correlation ID for request tracking.
     */
    @JsonProperty("correlationId")
    private String correlationId;

    /**
     * List of detection types that were requested.
     */
    @JsonProperty("detectionTypes")
    private List<String> detectionTypes;

    /**
     * List of detection results for each requested type.
     */
    @JsonProperty("results")
    private List<DetectionResponse> results;

    /**
     * Error message if the operation failed.
     */
    @JsonProperty("error")
    private String error;

    /**
     * Default constructor for JSON deserialization.
     */
    public MultipleDetectionResponse() {
    }

    /**
     * Constructs a new multiple detection response.
     *
     * @param correlationId  the correlation ID
     * @param detectionTypes the list of detection types
     * @param results        the list of detection results
     * @param error          the error message
     */
    public MultipleDetectionResponse(String correlationId, List<String> detectionTypes,
                                     List<DetectionResponse> results, String error) {
        this.correlationId = correlationId;
        this.detectionTypes = detectionTypes;
        this.results = results;
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
     * Gets the list of detection types.
     *
     * @return the list of detection types
     */
    public List<String> getDetectionTypes() {
        return detectionTypes;
    }

    /**
     * Sets the list of detection types.
     *
     * @param detectionTypes the list of detection types
     */
    public void setDetectionTypes(List<String> detectionTypes) {
        this.detectionTypes = detectionTypes;
    }

    /**
     * Gets the list of detection results.
     *
     * @return the list of detection results
     */
    public List<DetectionResponse> getResults() {
        return results;
    }

    /**
     * Sets the list of detection results.
     *
     * @param results the list of detection results
     */
    public void setResults(List<DetectionResponse> results) {
        this.results = results;
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
     * Checks if the response contains results.
     *
     * @return true if there are results, false otherwise
     */
    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }

    /**
     * Gets the total number of detections across all results.
     *
     * @return the total detection count
     */
    public int getTotalDetectionCount() {
        if (results == null) {
            return 0;
        }
        return results.stream()
            .mapToInt(DetectionResponse::getDetectionCount)
            .sum();
    }

    /**
     * Gets the average confidence across all results.
     *
     * @return the average confidence
     */
    public double getAverageConfidence() {
        if (results == null || results.isEmpty()) {
            return 0.0;
        }
        return results.stream()
            .mapToDouble(DetectionResponse::getAverageConfidence)
            .average()
            .orElse(0.0);
    }

    /**
     * Gets the total processing time across all results.
     *
     * @return the total processing time in milliseconds
     */
    public long getTotalProcessingTimeMs() {
        if (results == null) {
            return 0;
        }
        return results.stream()
            .mapToLong(DetectionResponse::getProcessingTimeMs)
            .sum();
    }

    /**
     * Builder for creating MultipleDetectionResponse instances.
     */
    public static class Builder {
        private String correlationId;
        private List<String> detectionTypes;
        private List<DetectionResponse> results;
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
         * Sets the list of detection types.
         *
         * @param detectionTypes the list of detection types
         * @return this builder
         */
        public Builder detectionTypes(List<String> detectionTypes) {
            this.detectionTypes = detectionTypes;
            return this;
        }

        /**
         * Sets the list of detection results.
         *
         * @param results the list of detection results
         * @return this builder
         */
        public Builder results(List<DetectionResponse> results) {
            this.results = results;
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
         * Builds a new MultipleDetectionResponse.
         *
         * @return the built MultipleDetectionResponse
         */
        public MultipleDetectionResponse build() {
            return new MultipleDetectionResponse(correlationId, detectionTypes, results, error);
        }
    }

    /**
     * Creates a new builder for MultipleDetectionResponse.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
