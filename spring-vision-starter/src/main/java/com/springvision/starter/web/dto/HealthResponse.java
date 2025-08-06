package com.springvision.starter.web.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for health check operations.
 *
 * <p>This DTO represents the response from health check operations, including
 * backend status, performance metrics, and supported capabilities.</p>
 *
 * <p>Example response:</p>
 * <pre>{@code
 * {
 *   "correlationId": "123e4567-e89b-12d3-a456-426614174000",
 *   "backendId": "opencv",
 *   "backendName": "OpenCV Vision Backend",
 *   "backendVersion": "4.8.1",
 *   "status": "HEALTHY",
 *   "statusMessage": "Backend is operating normally",
 *   "responseTimeMs": 45,
 *   "supportedDetectionTypes": ["face", "object"]
 * }
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionController
 */
public class HealthResponse {

    /**
     * Unique correlation ID for request tracking.
     */
    @JsonProperty("correlationId")
    private String correlationId;

    /**
     * The backend identifier.
     */
    @JsonProperty("backendId")
    private String backendId;

    /**
     * The backend display name.
     */
    @JsonProperty("backendName")
    private String backendName;

    /**
     * The backend version.
     */
    @JsonProperty("backendVersion")
    private String backendVersion;

    /**
     * The health status.
     */
    @JsonProperty("status")
    private String status;

    /**
     * The status message.
     */
    @JsonProperty("statusMessage")
    private String statusMessage;

    /**
     * Response time in milliseconds.
     */
    @JsonProperty("responseTimeMs")
    private long responseTimeMs;

    /**
     * List of supported detection types.
     */
    @JsonProperty("supportedDetectionTypes")
    private List<String> supportedDetectionTypes;

    /**
     * Error message if the health check failed.
     */
    @JsonProperty("error")
    private String error;

    /**
     * Default constructor for JSON deserialization.
     */
    public HealthResponse() {
    }

    /**
     * Constructs a new health response.
     *
     * @param correlationId the correlation ID
     * @param backendId the backend ID
     * @param backendName the backend name
     * @param backendVersion the backend version
     * @param status the status
     * @param statusMessage the status message
     * @param responseTimeMs the response time
     * @param supportedDetectionTypes the supported detection types
     * @param error the error message
     */
    public HealthResponse(String correlationId, String backendId, String backendName,
                         String backendVersion, String status, String statusMessage,
                         long responseTimeMs, List<String> supportedDetectionTypes, String error) {
        this.correlationId = correlationId;
        this.backendId = backendId;
        this.backendName = backendName;
        this.backendVersion = backendVersion;
        this.status = status;
        this.statusMessage = statusMessage;
        this.responseTimeMs = responseTimeMs;
        this.supportedDetectionTypes = supportedDetectionTypes;
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
     * Gets the backend ID.
     *
     * @return the backend ID
     */
    public String getBackendId() {
        return backendId;
    }

    /**
     * Sets the backend ID.
     *
     * @param backendId the backend ID
     */
    public void setBackendId(String backendId) {
        this.backendId = backendId;
    }

    /**
     * Gets the backend name.
     *
     * @return the backend name
     */
    public String getBackendName() {
        return backendName;
    }

    /**
     * Sets the backend name.
     *
     * @param backendName the backend name
     */
    public void setBackendName(String backendName) {
        this.backendName = backendName;
    }

    /**
     * Gets the backend version.
     *
     * @return the backend version
     */
    public String getBackendVersion() {
        return backendVersion;
    }

    /**
     * Sets the backend version.
     *
     * @param backendVersion the backend version
     */
    public void setBackendVersion(String backendVersion) {
        this.backendVersion = backendVersion;
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the status message.
     *
     * @return the status message
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Sets the status message.
     *
     * @param statusMessage the status message
     */
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    /**
     * Gets the response time in milliseconds.
     *
     * @return the response time
     */
    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    /**
     * Sets the response time in milliseconds.
     *
     * @param responseTimeMs the response time
     */
    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    /**
     * Gets the list of supported detection types.
     *
     * @return the list of supported detection types
     */
    public List<String> getSupportedDetectionTypes() {
        return supportedDetectionTypes;
    }

    /**
     * Sets the list of supported detection types.
     *
     * @param supportedDetectionTypes the list of supported detection types
     */
    public void setSupportedDetectionTypes(List<String> supportedDetectionTypes) {
        this.supportedDetectionTypes = supportedDetectionTypes;
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
     * Checks if the backend is healthy.
     *
     * @return true if the backend is healthy, false otherwise
     */
    public boolean isHealthy() {
        return "HEALTHY".equals(status) || "UP".equals(status);
    }

    /**
     * Builder for creating HealthResponse instances.
     */
    public static class Builder {
        private String correlationId;
        private String backendId;
        private String backendName;
        private String backendVersion;
        private String status;
        private String statusMessage;
        private long responseTimeMs;
        private List<String> supportedDetectionTypes;
        private String error;

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
         * Sets the backend ID.
         *
         * @param backendId the backend ID
         * @return this builder
         */
        public Builder backendId(String backendId) {
            this.backendId = backendId;
            return this;
        }

        /**
         * Sets the backend name.
         *
         * @param backendName the backend name
         * @return this builder
         */
        public Builder backendName(String backendName) {
            this.backendName = backendName;
            return this;
        }

        /**
         * Sets the backend version.
         *
         * @param backendVersion the backend version
         * @return this builder
         */
        public Builder backendVersion(String backendVersion) {
            this.backendVersion = backendVersion;
            return this;
        }

        /**
         * Sets the status.
         *
         * @param status the status
         * @return this builder
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the status message.
         *
         * @param statusMessage the status message
         * @return this builder
         */
        public Builder statusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
            return this;
        }

        /**
         * Sets the response time in milliseconds.
         *
         * @param responseTimeMs the response time
         * @return this builder
         */
        public Builder responseTimeMs(long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
            return this;
        }

        /**
         * Sets the list of supported detection types.
         *
         * @param supportedDetectionTypes the list of supported detection types
         * @return this builder
         */
        public Builder supportedDetectionTypes(List<String> supportedDetectionTypes) {
            this.supportedDetectionTypes = supportedDetectionTypes;
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
         * Builds a new HealthResponse.
         *
         * @return the built HealthResponse
         */
        public HealthResponse build() {
            return new HealthResponse(correlationId, backendId, backendName, backendVersion,
                status, statusMessage, responseTimeMs, supportedDetectionTypes, error);
        }
    }

    /**
     * Creates a new builder for HealthResponse.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
