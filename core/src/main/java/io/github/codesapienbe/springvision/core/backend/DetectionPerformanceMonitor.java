package io.github.codesapienbe.springvision.core.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring for face detection operations.
 *
 * <p>Tracks various metrics to help optimize detection performance:</p>
 * <ul>
 *   <li>Detection latency per detector (YuNet, DNN, Haar)</li>
 *   <li>Cache hit rates</li>
 *   <li>Detection counts and success rates</li>
 *   <li>Memory usage statistics</li>
 * </ul>
 *
 * <p>This information is valuable for:</p>
 * <ul>
 *   <li>Identifying performance bottlenecks</li>
 *   <li>Tuning detector selection strategies</li>
 *   <li>Monitoring production performance</li>
 *   <li>Capacity planning</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
public class DetectionPerformanceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(DetectionPerformanceMonitor.class);

    // Detection counts
    private final AtomicInteger totalDetections = new AtomicInteger(0);
    private final AtomicInteger successfulDetections = new AtomicInteger(0);
    private final AtomicInteger failedDetections = new AtomicInteger(0);

    // Timing statistics per detector
    private final ConcurrentHashMap<String, AtomicLong> detectorTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> detectorCounts = new ConcurrentHashMap<>();

    // Face counts
    private final AtomicLong totalFacesDetected = new AtomicLong(0);

    // Cache statistics
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);

    /**
     * Records the start of a detection operation.
     *
     * @return timestamp in nanoseconds
     */
    public long startDetection() {
        totalDetections.incrementAndGet();
        return System.nanoTime();
    }

    /**
     * Records the completion of a successful detection.
     *
     * @param startTime    start timestamp from startDetection()
     * @param detectorName name of the detector used
     * @param facesFound   number of faces detected
     */
    public void recordSuccess(long startTime, String detectorName, int facesFound) {
        successfulDetections.incrementAndGet();
        totalFacesDetected.addAndGet(facesFound);

        long elapsedNanos = System.nanoTime() - startTime;
        recordDetectorTime(detectorName, elapsedNanos);
    }

    /**
     * Records a failed detection operation.
     *
     * @param startTime    start timestamp from startDetection()
     * @param detectorName name of the detector used
     */
    public void recordFailure(long startTime, String detectorName) {
        failedDetections.incrementAndGet();

        long elapsedNanos = System.nanoTime() - startTime;
        recordDetectorTime(detectorName, elapsedNanos);
    }

    /**
     * Records timing for a specific detector.
     *
     * @param detectorName name of the detector
     * @param elapsedNanos elapsed time in nanoseconds
     */
    private void recordDetectorTime(String detectorName, long elapsedNanos) {
        detectorTimes.computeIfAbsent(detectorName, k -> new AtomicLong(0))
            .addAndGet(elapsedNanos);
        detectorCounts.computeIfAbsent(detectorName, k -> new AtomicInteger(0))
            .incrementAndGet();
    }

    /**
     * Records a cache hit.
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    /**
     * Records a cache miss.
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    /**
     * Gets the average detection time for a specific detector.
     *
     * @param detectorName name of the detector
     * @return average time in milliseconds
     */
    public double getAverageDetectorTime(String detectorName) {
        AtomicLong totalTime = detectorTimes.get(detectorName);
        AtomicInteger count = detectorCounts.get(detectorName);

        if (totalTime == null || count == null || count.get() == 0) {
            return 0.0;
        }

        return (double) totalTime.get() / count.get() / 1_000_000.0; // Convert to ms
    }

    /**
     * Gets the cache hit rate as a percentage.
     *
     * @return cache hit rate (0-100)
     */
    public double getCacheHitRate() {
        int hits = cacheHits.get();
        int misses = cacheMisses.get();
        int total = hits + misses;

        if (total == 0) return 0.0;

        return (double) hits / total * 100.0;
    }

    /**
     * Gets the success rate as a percentage.
     *
     * @return success rate (0-100)
     */
    public double getSuccessRate() {
        int total = totalDetections.get();
        if (total == 0) return 0.0;

        return (double) successfulDetections.get() / total * 100.0;
    }

    /**
     * Gets the average number of faces detected per successful detection.
     *
     * @return average faces per detection
     */
    public double getAverageFacesPerDetection() {
        int successful = successfulDetections.get();
        if (successful == 0) return 0.0;

        return (double) totalFacesDetected.get() / successful;
    }

    /**
     * Logs a comprehensive performance report.
     */
    public void logPerformanceReport() {
        logger.info("=== Face Detection Performance Report ===");
        logger.info("Total detections: {}", totalDetections.get());
        logger.info("Successful: {} ({:.2f}%)", successfulDetections.get(), getSuccessRate());
        logger.info("Failed: {}", failedDetections.get());
        logger.info("Total faces detected: {}", totalFacesDetected.get());
        logger.info("Average faces per detection: {:.2f}", getAverageFacesPerDetection());
        logger.info("Cache hit rate: {:.2f}%", getCacheHitRate());

        logger.info("--- Detector Performance ---");
        for (String detector : detectorCounts.keySet()) {
            logger.info("{}: {} runs, avg time: {:.2f}ms",
                detector,
                detectorCounts.get(detector).get(),
                getAverageDetectorTime(detector));
        }
        logger.info("========================================");
    }

    /**
     * Resets all statistics.
     */
    public void reset() {
        totalDetections.set(0);
        successfulDetections.set(0);
        failedDetections.set(0);
        totalFacesDetected.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        detectorTimes.clear();
        detectorCounts.clear();
    }

    /**
     * Gets a summary string of current statistics.
     *
     * @return summary string
     */
    public String getSummary() {
        return String.format(
            "Detections: %d (%.1f%% success), Faces: %d (avg %.2f/detection), Cache: %.1f%% hit rate",
            totalDetections.get(),
            getSuccessRate(),
            totalFacesDetected.get(),
            getAverageFacesPerDetection(),
            getCacheHitRate()
        );
    }
}

