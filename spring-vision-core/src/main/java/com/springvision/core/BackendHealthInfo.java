package com.springvision.core;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of backend health information.
 *
 * <p>This record encapsulates comprehensive health information about a vision
 * backend, including its status, error details if unhealthy, and relevant
 * metrics. It provides a standardized way to monitor and diagnose backend
 * health across different implementations.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * VisionBackend backend = // ... get backend
 * BackendHealthInfo healthInfo = backend.getHealthInfo();
 *
 * if (healthInfo.isHealthy()) {
 *     logger.info("Backend is healthy: {}", healthInfo.getStatus());
 * } else {
 *     logger.error("Backend is unhealthy: {}", healthInfo.getErrorMessage());
 * }
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionBackend
 */
public record BackendHealthInfo(
    String backendId,
    HealthStatus status,
    String statusMessage,
    String errorMessage,
    Instant lastCheckTime,
    long responseTimeMs,
    Map<String, Object> metrics
) {

    /**
     * Health status enumeration for vision backends.
     */
    public enum HealthStatus {
        /**
         * Backend is healthy and ready to process requests.
         */
        HEALTHY("healthy"),

        /**
         * Backend is unhealthy and cannot process requests.
         */
        UNHEALTHY("unhealthy"),

        /**
         * Backend status is unknown or cannot be determined.
         */
        UNKNOWN("unknown"),

        /**
         * Backend is starting up and not yet ready.
         */
        STARTING("starting"),

        /**
         * Backend is shutting down and cannot accept new requests.
         */
        SHUTTING_DOWN("shutting_down");

        private final String value;

        HealthStatus(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Creates a new BackendHealthInfo with the specified parameters.
     *
     * @param backendId the backend identifier
     * @param status the health status
     * @param statusMessage a descriptive status message
     * @param errorMessage error details if unhealthy, or null if healthy
     * @param lastCheckTime when the health check was performed
     * @param responseTimeMs the response time of the health check in milliseconds
     * @param metrics additional health metrics
     */
    public BackendHealthInfo {
        Objects.requireNonNull(backendId, "Backend ID must not be null");
        Objects.requireNonNull(status, "Health status must not be null");
        Objects.requireNonNull(statusMessage, "Status message must not be null");
        Objects.requireNonNull(lastCheckTime, "Last check time must not be null");
        Objects.requireNonNull(metrics, "Metrics must not be null");

        if (backendId.trim().isEmpty()) {
            throw new IllegalArgumentException("Backend ID must not be empty");
        }

        if (statusMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Status message must not be empty");
        }

        if (responseTimeMs < 0) {
            throw new IllegalArgumentException("Response time must be non-negative");
        }
    }

    /**
     * Creates a healthy BackendHealthInfo.
     *
     * @param backendId the backend identifier
     * @param statusMessage a descriptive status message
     * @param responseTimeMs the response time in milliseconds
     * @return a healthy BackendHealthInfo instance
     */
    public static BackendHealthInfo healthy(String backendId, String statusMessage, long responseTimeMs) {
        return new BackendHealthInfo(backendId, HealthStatus.HEALTHY, statusMessage, null,
                                   Instant.now(), responseTimeMs, Map.of());
    }

    /**
     * Creates a healthy BackendHealthInfo with metrics.
     *
     * @param backendId the backend identifier
     * @param statusMessage a descriptive status message
     * @param responseTimeMs the response time in milliseconds
     * @param metrics additional health metrics
     * @return a healthy BackendHealthInfo instance
     */
    public static BackendHealthInfo healthy(String backendId, String statusMessage,
                                          long responseTimeMs, Map<String, Object> metrics) {
        return new BackendHealthInfo(backendId, HealthStatus.HEALTHY, statusMessage, null,
                                   Instant.now(), responseTimeMs, metrics);
    }

    /**
     * Creates an unhealthy BackendHealthInfo.
     *
     * @param backendId the backend identifier
     * @param statusMessage a descriptive status message
     * @param errorMessage the error details
     * @param responseTimeMs the response time in milliseconds
     * @return an unhealthy BackendHealthInfo instance
     */
    public static BackendHealthInfo unhealthy(String backendId, String statusMessage,
                                            String errorMessage, long responseTimeMs) {
        return new BackendHealthInfo(backendId, HealthStatus.UNHEALTHY, statusMessage, errorMessage,
                                   Instant.now(), responseTimeMs, Map.of());
    }

    /**
     * Creates an unhealthy BackendHealthInfo with metrics.
     *
     * @param backendId the backend identifier
     * @param statusMessage a descriptive status message
     * @param errorMessage the error details
     * @param responseTimeMs the response time in milliseconds
     * @param metrics additional health metrics
     * @return an unhealthy BackendHealthInfo instance
     */
    public static BackendHealthInfo unhealthy(String backendId, String statusMessage,
                                            String errorMessage, long responseTimeMs,
                                            Map<String, Object> metrics) {
        return new BackendHealthInfo(backendId, HealthStatus.UNHEALTHY, statusMessage, errorMessage,
                                   Instant.now(), responseTimeMs, metrics);
    }

    /**
     * Creates an unknown BackendHealthInfo.
     *
     * @param backendId the backend identifier
     * @param statusMessage a descriptive status message
     * @return an unknown BackendHealthInfo instance
     */
    public static BackendHealthInfo unknown(String backendId, String statusMessage) {
        return new BackendHealthInfo(backendId, HealthStatus.UNKNOWN, statusMessage, null,
                                   Instant.now(), 0, Map.of());
    }

    /**
     * Checks if the backend is healthy.
     *
     * @return true if the status is HEALTHY, false otherwise
     */
    public boolean isHealthy() {
        return status == HealthStatus.HEALTHY;
    }

    /**
     * Checks if the backend is unhealthy.
     *
     * @return true if the status is UNHEALTHY, false otherwise
     */
    public boolean isUnhealthy() {
        return status == HealthStatus.UNHEALTHY;
    }

    /**
     * Checks if the backend status is unknown.
     *
     * @return true if the status is UNKNOWN, false otherwise
     */
    public boolean isUnknown() {
        return status == HealthStatus.UNKNOWN;
    }

    /**
     * Checks if the backend is starting up.
     *
     * @return true if the status is STARTING, false otherwise
     */
    public boolean isStarting() {
        return status == HealthStatus.STARTING;
    }

    /**
     * Checks if the backend is shutting down.
     *
     * @return true if the status is SHUTTING_DOWN, false otherwise
     */
    public boolean isShuttingDown() {
        return status == HealthStatus.SHUTTING_DOWN;
    }

    /**
     * Gets the response time in seconds.
     *
     * @return the response time in seconds
     */
    public double getResponseTimeSeconds() {
        return responseTimeMs / 1000.0;
    }

    /**
     * Checks if the response time is within acceptable limits.
     *
     * @param maxResponseTimeMs the maximum acceptable response time in milliseconds
     * @return true if response time is within limits, false otherwise
     */
    public boolean isResponseTimeAcceptable(long maxResponseTimeMs) {
        return responseTimeMs <= maxResponseTimeMs;
    }

    /**
     * Gets a metric value by key.
     *
     * @param key the metric key
     * @return the metric value, or null if not found
     */
    public Object getMetric(String key) {
        return metrics.get(key);
    }

    /**
     * Gets a metric value by key with a default value.
     *
     * @param key the metric key
     * @param defaultValue the default value to return if key not found
     * @return the metric value, or the default value if not found
     */
    public Object getMetric(String key, Object defaultValue) {
        return metrics.getOrDefault(key, defaultValue);
    }

    /**
     * Checks if this health info contains a specific metric.
     *
     * @param key the metric key to check
     * @return true if the metric exists, false otherwise
     */
    public boolean hasMetric(String key) {
        return metrics.containsKey(key);
    }

    /**
     * Gets the number of metrics available.
     *
     * @return the number of metrics
     */
    public int getMetricCount() {
        return metrics.size();
    }

    @Override
    public String toString() {
        return String.format("BackendHealthInfo{backendId='%s', status=%s, message='%s', " +
                           "responseTime=%dms, metrics=%d}",
            backendId, status, statusMessage, responseTimeMs, metrics.size());
    }
}
