package io.github.codesapienbe.springvision.core.config;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.util.Assert;

import io.github.codesapienbe.springvision.core.BackendHealthInfo;
import io.github.codesapienbe.springvision.core.VisionBackend;

/**
 * Health indicator for Spring Vision backend.
 *
 * <p>This health indicator monitors the health of the configured vision backend
 * and reports its status to Spring Boot Actuator health endpoints. It provides
 * detailed health information including backend status, response times, and
 * any error details.</p>
 *
 * <p>The health indicator is automatically configured when Spring Boot Actuator
 * is present and vision health monitoring is enabled.</p>
 *
 * <p>Example health endpoint response:</p>
 * <pre>{@code
 * {
 *   "status": "UP",
 *   "components": {
 *     "vision": {
 *       "status": "UP",
 *       "details": {
 *         "backend": "opencv",
 *         "status": "healthy",
 *         "responseTime": 45,
 *         "lastCheck": "2024-01-15T10:30:00Z"
 *       }
 *     }
 *   }
 * }
 * }</pre>
 *
 * @author Spring Vision Team
 * @see VisionAutoConfiguration
 * @see VisionProperties
 * @since 1.0.0
 */
public class VisionHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(VisionHealthIndicator.class);

    /**
     * Health indicator name.
     */
    public static final String HEALTH_INDICATOR_NAME = "vision";

    /**
     * Custom {@link Status} indicating that the service is operational but performing below the
     * expected threshold (e.g., higher latency). Spring Boot does not provide a built-in
     * DEGRADED status, so we create one to preserve backward-compatibility with previous
     * versions of the library.
     */
    private static final Status DEGRADED = new Status("DEGRADED");

    /**
     * Maximum response time threshold for health checks (5 seconds).
     */
    private static final long MAX_RESPONSE_TIME_THRESHOLD = 5000;

    /**
     * The vision backend to monitor.
     */
    private final VisionBackend backend;

    /**
     * The vision configuration properties.
     */
    private final VisionProperties properties;

    /**
     * Last health check time.
     */
    private Instant lastHealthCheck;

    /**
     * Last health check result.
     */
    private BackendHealthInfo lastHealthInfo;

    /**
     * Constructs a new vision health indicator.
     *
     * @param backend    the vision backend to monitor
     * @param properties the vision configuration properties
     * @throws IllegalArgumentException if backend is null
     */
    public VisionHealthIndicator(VisionBackend backend, VisionProperties properties) {
        Assert.notNull(backend, "Vision backend must not be null");
        Assert.notNull(properties, "Vision properties must not be null");

        this.backend = backend;
        this.properties = properties;
        this.lastHealthCheck = Instant.now();
        this.lastHealthInfo = null;

        logger.debug("Vision health indicator initialized for backend: {}", backend.getBackendId());
    }

    /**
     * Constructs a new vision health indicator with default properties.
     *
     * @param backend the vision backend to monitor
     * @throws IllegalArgumentException if backend is null
     */
    public VisionHealthIndicator(VisionBackend backend) {
        this(backend, new VisionProperties());
    }

    @Override
    public Health health() {
        logger.debug("Performing vision health check");

        Instant startTime = Instant.now();

        try {
            // Get health information from the backend
            BackendHealthInfo healthInfo = backend.getHealthInfo();
            lastHealthInfo = healthInfo;
            lastHealthCheck = startTime;

            // Calculate response time
            Duration responseTime = Duration.between(startTime, Instant.now());
            long responseTimeMs = responseTime.toMillis();

            // Determine health status
            Status status = determineHealthStatus(healthInfo, responseTimeMs);

            // Build health details
            Map<String, Object> details = buildHealthDetails(healthInfo, responseTimeMs);

            // Create health response
            Health.Builder healthBuilder = Health.status(status);

            // Add details
            details.forEach(healthBuilder::withDetail);

            // Add additional context if unhealthy
            if (status != Status.UP) {
                healthBuilder.withDetail("lastHealthyCheck", lastHealthCheck);
                healthBuilder.withDetail("backendId", backend.getBackendId());
                healthBuilder.withDetail("backendVersion", backend.getVersion());
            }

            Health health = healthBuilder.build();

            logger.debug("Vision health check completed: status={}, responseTime={}ms",
                status, responseTimeMs);

            return health;

        } catch (Exception e) {
            logger.error("Vision health check failed", e);

            // Return unhealthy status with error details
            return Health.down()
                .withDetail("error", e.getClass().getSimpleName())
                .withDetail("message", e.getMessage())
                .withDetail("backendId", backend.getBackendId())
                .withDetail("lastCheck", startTime)
                .build();
        }
    }

    /**
     * Determines the health status based on backend health info and response time.
     *
     * @param healthInfo     the backend health information
     * @param responseTimeMs the response time in milliseconds
     * @return the determined health status
     */
    private Status determineHealthStatus(BackendHealthInfo healthInfo, long responseTimeMs) {
        // Check if backend is healthy
        if (!healthInfo.isHealthy()) {
            return Status.DOWN;
        }

        // Check response time threshold
        long maxResponseTime = properties.getHealth().getMaxResponseTime();
        if (responseTimeMs > maxResponseTime) {
            return DEGRADED;
        }

        // Check if response time is acceptable
        if (responseTimeMs > MAX_RESPONSE_TIME_THRESHOLD) {
            return DEGRADED;
        }

        return Status.UP;
    }

    /**
     * Builds health details map from backend health information.
     *
     * @param healthInfo     the backend health information
     * @param responseTimeMs the response time in milliseconds
     * @return the health details map
     */
    private Map<String, Object> buildHealthDetails(BackendHealthInfo healthInfo, long responseTimeMs) {
        return Map.of(
            "backend", backend.getBackendId(),
            "backendName", backend.getDisplayName(),
            "backendVersion", backend.getVersion(),
            "status", healthInfo.status().toString(),
            "statusMessage", healthInfo.statusMessage(),
            "responseTime", responseTimeMs,
            "responseTimeSeconds", responseTimeMs / 1000.0,
            "lastCheck", healthInfo.lastCheckTime(),
            "supportedDetectionTypes", backend.getSupportedDetectionTypes().size(),
            "metricsCount", healthInfo.getMetricCount()
        );
    }

    /**
     * Gets the last health check time.
     *
     * @return the last health check time
     */
    public Instant getLastHealthCheck() {
        return lastHealthCheck;
    }

    /**
     * Gets the last health information.
     *
     * @return the last health information, or null if no health check has been performed
     */
    public BackendHealthInfo getLastHealthInfo() {
        return lastHealthInfo;
    }

    /**
     * Gets the monitored backend.
     *
     * @return the vision backend
     */
    public VisionBackend getBackend() {
        return backend;
    }

    /**
     * Gets the vision configuration properties.
     *
     * @return the vision properties
     */
    public VisionProperties getProperties() {
        return properties;
    }

    /**
     * Checks if the backend is currently healthy.
     *
     * @return true if the backend is healthy, false otherwise
     */
    public boolean isHealthy() {
        try {
            return backend.isHealthy();
        } catch (Exception e) {
            logger.warn("Failed to check backend health", e);
            return false;
        }
    }

    /**
     * Gets the current backend health information.
     *
     * @return the current health information
     */
    public BackendHealthInfo getCurrentHealthInfo() {
        try {
            return backend.getHealthInfo();
        } catch (Exception e) {
            logger.warn("Failed to get backend health info", e);
            return BackendHealthInfo.unknown(backend.getBackendId(),
                "Failed to get health info: " + e.getMessage());
        }
    }

    /**
     * Performs a detailed health check and returns the result.
     *
     * <p>This method performs a comprehensive health check including
     * backend status, response time, and supported capabilities.</p>
     *
     * @return detailed health check result
     */
    public DetailedHealthCheck performDetailedHealthCheck() {
        Instant startTime = Instant.now();

        try {
            // Check basic health
            boolean isHealthy = backend.isHealthy();

            // Get detailed health info
            BackendHealthInfo healthInfo = backend.getHealthInfo();

            // Calculate response time
            Duration responseTime = Duration.between(startTime, Instant.now());

            // Check supported detection types
            int supportedTypesCount = backend.getSupportedDetectionTypes().size();

            return new DetailedHealthCheck(
                isHealthy,
                healthInfo,
                responseTime,
                supportedTypesCount,
                startTime
            );

        } catch (Exception e) {
            logger.error("Detailed health check failed", e);

            Duration responseTime = Duration.between(startTime, Instant.now());

            return new DetailedHealthCheck(
                false,
                BackendHealthInfo.unknown(backend.getBackendId(), e.getMessage()),
                responseTime,
                0,
                startTime
            );
        }
    }

    /**
     * Detailed health check result.
     */
    public static class DetailedHealthCheck {

        private final boolean healthy;
        private final BackendHealthInfo healthInfo;
        private final Duration responseTime;
        private final int supportedTypesCount;
        private final Instant checkTime;

        public DetailedHealthCheck(boolean healthy, BackendHealthInfo healthInfo,
                                   Duration responseTime, int supportedTypesCount, Instant checkTime) {
            this.healthy = healthy;
            this.healthInfo = healthInfo;
            this.responseTime = responseTime;
            this.supportedTypesCount = supportedTypesCount;
            this.checkTime = checkTime;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public BackendHealthInfo getHealthInfo() {
            return healthInfo;
        }

        public Duration getResponseTime() {
            return responseTime;
        }

        public int getSupportedTypesCount() {
            return supportedTypesCount;
        }

        public Instant getCheckTime() {
            return checkTime;
        }
    }
}
