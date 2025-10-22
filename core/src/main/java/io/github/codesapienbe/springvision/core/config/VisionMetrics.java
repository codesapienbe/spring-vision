package io.github.codesapienbe.springvision.core.config;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Metrics collection for Spring Vision framework.
 *
 * <p>This class provides comprehensive metrics for monitoring vision operations
 * including detection counts, processing times, error rates, and performance
 * indicators. It integrates with Micrometer for metrics collection and can
 * be exported to various monitoring systems.</p>
 *
 * <p>Metrics are automatically collected when the vision backend is used
 * and metrics collection is enabled in the configuration.</p>
 *
 * <p>Example metrics:</p>
 * <ul>
 *   <li>vision.detections.total - Total number of detections performed</li>
 *   <li>vision.detections.face - Number of face detections</li>
 *   <li>vision.detections.object - Number of object detections</li>
 *   <li>vision.processing.time - Processing time for detections</li>
 *   <li>vision.errors.total - Total number of errors</li>
 *   <li>vision.backend.health - Backend health status</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @see VisionAutoConfiguration
 * @see VisionProperties
 * @since 1.0.0
 */
public class VisionMetrics {

    private static final Logger logger = LoggerFactory.getLogger(VisionMetrics.class);

    /**
     * Metrics prefix for all vision metrics.
     */
    public static final String METRICS_PREFIX = "vision";

    /**
     * The vision backend to monitor.
     */
    private final VisionBackend backend;

    /**
     * The meter registry for metrics collection.
     */
    private final MeterRegistry meterRegistry;

    /**
     * The vision configuration properties.
     */
    private final VisionProperties properties;

    /**
     * Detection counters by type.
     */
    private final Map<DetectionType, Counter> detectionCounters;

    /**
     * Processing time timers by type.
     */
    private final Map<DetectionType, Timer> processingTimers;

    /**
     * Error counters by type.
     */
    private final Map<DetectionType, Counter> errorCounters;

    /**
     * Total detections counter.
     */
    private Counter totalDetectionsCounter;

    /**
     * Total errors counter.
     */
    private Counter totalErrorsCounter;

    /**
     * Backend health gauge.
     */
    private io.micrometer.core.instrument.Gauge backendHealthGauge;

    /**
     * Last metrics collection time.
     */
    private Instant lastMetricsCollection;

    /**
     * Constructs a new vision metrics collector.
     *
     * @param backend       the vision backend to monitor
     * @param meterRegistry the meter registry for metrics collection
     * @param properties    the vision configuration properties
     * @throws IllegalArgumentException if any parameter is null
     */
    public VisionMetrics(VisionBackend backend, MeterRegistry meterRegistry, VisionProperties properties) {
        Assert.notNull(backend, "Vision backend must not be null");
        Assert.notNull(meterRegistry, "Meter registry must not be null");
        Assert.notNull(properties, "Vision properties must not be null");

        this.backend = backend;
        this.meterRegistry = meterRegistry;
        this.properties = properties;
        this.detectionCounters = new ConcurrentHashMap<>();
        this.processingTimers = new ConcurrentHashMap<>();
        this.errorCounters = new ConcurrentHashMap<>();
        this.lastMetricsCollection = Instant.now();

        // Initialize basic metrics first
        initializeBasicMetrics();
        // Initialize gauge metrics after construction to avoid 'this' escape
        initializeGaugeMetrics();

        logger.debug("Vision metrics initialized for backend: {}", backend.getBackendId());
    }

    /**
     * Constructs a new vision metrics collector with default properties.
     *
     * @param backend       the vision backend to monitor
     * @param meterRegistry the meter registry for metrics collection
     * @throws IllegalArgumentException if any parameter is null
     */
    public VisionMetrics(VisionBackend backend, MeterRegistry meterRegistry) {
        this(backend, meterRegistry, new VisionProperties());
    }

    /**
     * Records a successful detection operation.
     *
     * @param detectionType    the type of detection performed
     * @param processingTimeMs the processing time in milliseconds
     * @param detectionCount   the number of detections found
     */
    public void recordDetection(DetectionType detectionType, long processingTimeMs, int detectionCount) {
        try {
            // Increment detection counter
            Counter counter = getDetectionCounter(detectionType);
            counter.increment(detectionCount);

            // Increment total detections counter
            totalDetectionsCounter.increment(detectionCount);

            // Record processing time
            Timer timer = getProcessingTimer(detectionType);
            timer.record(processingTimeMs, TimeUnit.MILLISECONDS);

            logger.debug("Recorded detection: type={}, count={}, time={}ms",
                detectionType, detectionCount, processingTimeMs);

        } catch (Exception e) {
            logger.warn("Failed to record detection metrics", e);
        }
    }

    /**
     * Records a detection error.
     *
     * @param detectionType the type of detection that failed
     * @param error         the error that occurred
     */
    public void recordError(DetectionType detectionType, Throwable error) {
        try {
            // Increment error counter for specific detection type
            Counter errorCounter = getErrorCounter(detectionType);
            errorCounter.increment();

            // Increment total errors counter
            totalErrorsCounter.increment();

            logger.debug("Recorded detection error: type={}, error={}",
                detectionType, error.getClass().getSimpleName());

        } catch (Exception e) {
            logger.warn("Failed to record error metrics", e);
        }
    }

    /**
     * Records a backend error.
     *
     * @param error the error that occurred
     */
    public void recordBackendError(Throwable error) {
        try {
            // Increment total errors counter
            totalErrorsCounter.increment();

            logger.debug("Recorded backend error: {}", error.getClass().getSimpleName());

        } catch (Exception e) {
            logger.warn("Failed to record backend error metrics", e);
        }
    }

    /**
     * Updates backend health metrics.
     */
    public void updateBackendHealth() {
        try {
            // The health gauge is automatically updated by the gauge function
            // This method can be called to trigger an update
            logger.debug("Updated backend health metrics");

        } catch (Exception e) {
            logger.warn("Failed to update backend health metrics", e);
        }
    }

    /**
     * Collects and records comprehensive metrics.
     *
     * <p>This method performs a comprehensive metrics collection including
     * backend health, performance indicators, and operational statistics.</p>
     */
    public void collectMetrics() {
        try {
            Instant startTime = Instant.now();

            // Update backend health
            updateBackendHealth();

            // Record metrics collection time
            long collectionTime = Duration.between(startTime, Instant.now()).toMillis();
            lastMetricsCollection = Instant.now();

            logger.debug("Collected vision metrics in {}ms", collectionTime);

        } catch (Exception e) {
            logger.warn("Failed to collect metrics", e);
        }
    }

    /**
     * Gets the detection counter for the specified detection type.
     *
     * @param detectionType the detection type
     * @return the detection counter
     */
    private Counter getDetectionCounter(DetectionType detectionType) {
        return detectionCounters.computeIfAbsent(detectionType, type -> {
            return Counter.builder(METRICS_PREFIX + ".detections")
                .tag("type", type.getCode())
                .tag("backend", backend.getBackendId())
                .description("Number of " + type.getDisplayName() + " detections")
                .register(meterRegistry);
        });
    }

    /**
     * Gets the processing timer for the specified detection type.
     *
     * @param detectionType the detection type
     * @return the processing timer
     */
    private Timer getProcessingTimer(DetectionType detectionType) {
        return processingTimers.computeIfAbsent(detectionType, type -> {
            return Timer.builder(METRICS_PREFIX + ".processing.time")
                .tag("type", type.getCode())
                .tag("backend", backend.getBackendId())
                .description("Processing time for " + type.getDisplayName() + " detections")
                .register(meterRegistry);
        });
    }

    /**
     * Gets the error counter for the specified detection type.
     *
     * @param detectionType the detection type
     * @return the error counter
     */
    private Counter getErrorCounter(DetectionType detectionType) {
        return errorCounters.computeIfAbsent(detectionType, type -> {
            return Counter.builder(METRICS_PREFIX + ".errors")
                .tag("type", type.getCode())
                .tag("backend", backend.getBackendId())
                .description("Number of errors for " + type.getDisplayName() + " detections")
                .register(meterRegistry);
        });
    }

    /**
     * Initializes all metrics.
     */
    private void initializeBasicMetrics() {
        // Total detections counter
        totalDetectionsCounter = Counter.builder(METRICS_PREFIX + ".detections.total")
            .tag("backend", backend.getBackendId())
            .description("Total number of detections performed")
            .register(meterRegistry);

        // Total errors counter
        totalErrorsCounter = Counter.builder(METRICS_PREFIX + ".errors.total")
            .tag("backend", backend.getBackendId())
            .description("Total number of errors")
            .register(meterRegistry);

        logger.debug("Initialized basic vision metrics for backend: {}", backend.getBackendId());
    }

    private void initializeGaugeMetrics() {
        // Backend health gauge - use method reference to avoid 'this' escape during construction
        backendHealthGauge = io.micrometer.core.instrument.Gauge.builder(METRICS_PREFIX + ".backend.health", this, VisionMetrics::getBackendHealthValue)
            .tag("backend", backend.getBackendId())
            .description("Backend health status (1 = healthy, 0 = unhealthy)")
            .register(meterRegistry);

        logger.debug("Initialized gauge metrics for backend: {}", backend.getBackendId());
    }

    /**
     * Gets the backend health value for the gauge.
     *
     * @param metrics the metrics instance
     * @return 1.0 if healthy, 0.0 if unhealthy
     */
    private static double getBackendHealthValue(VisionMetrics metrics) {
        try {
            return metrics.backend.isHealthy() ? 1.0 : 0.0;
        } catch (Exception e) {
            logger.warn("Failed to get backend health value", e);
            return 0.0;
        }
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
     * Gets the meter registry.
     *
     * @return the meter registry
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
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
     * Gets the last metrics collection time.
     *
     * @return the last metrics collection time
     */
    public Instant getLastMetricsCollection() {
        return lastMetricsCollection;
    }

    /**
     * Gets the total detections counter.
     *
     * @return the total detections counter
     */
    public Counter getTotalDetectionsCounter() {
        return totalDetectionsCounter;
    }

    /**
     * Gets the total errors counter.
     *
     * @return the total errors counter
     */
    public Counter getTotalErrorsCounter() {
        return totalErrorsCounter;
    }

    /**
     * Gets the backend health gauge.
     *
     * @return the backend health gauge
     */
    public io.micrometer.core.instrument.Gauge getBackendHealthGauge() {
        return backendHealthGauge;
    }

    /**
     * Gets a snapshot of current metrics.
     *
     * @return metrics snapshot
     */
    public MetricsSnapshot getMetricsSnapshot() {
        return new MetricsSnapshot(
            totalDetectionsCounter.count(),
            totalErrorsCounter.count(),
            lastMetricsCollection,
            backend.isHealthy()
        );
    }

    /**
     * Metrics snapshot containing current metric values.
     */
    public static class MetricsSnapshot {

        private final double totalDetections;
        private final double totalErrors;
        private final Instant lastCollection;
        private final boolean backendHealthy;

        public MetricsSnapshot(double totalDetections, double totalErrors,
                               Instant lastCollection, boolean backendHealthy) {
            this.totalDetections = totalDetections;
            this.totalErrors = totalErrors;
            this.lastCollection = lastCollection;
            this.backendHealthy = backendHealthy;
        }

        public double getTotalDetections() {
            return totalDetections;
        }

        public double getTotalErrors() {
            return totalErrors;
        }

        public Instant getLastCollection() {
            return lastCollection;
        }

        public boolean isBackendHealthy() {
            return backendHealthy;
        }

        public double getErrorRate() {
            if (totalDetections == 0) {
                return 0.0;
            }
            return totalErrors / totalDetections;
        }
    }
}
