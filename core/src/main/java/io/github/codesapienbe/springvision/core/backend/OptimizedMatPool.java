package io.github.codesapienbe.springvision.core.backend;

import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe Mat object pool for reducing allocation overhead in OpenCV operations.
 *
 * <p>This pool maintains a cache of reusable Mat objects to avoid the performance
 * penalty of frequent allocations and deallocations during image processing.
 *
 * <p>Performance benefits:</p>
 * <ul>
 *   <li>Reduces garbage collection pressure</li>
 *   <li>Eliminates native memory allocation overhead</li>
 *   <li>Thread-safe design for concurrent operations</li>
 *   <li>Automatic statistics tracking for monitoring</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
public class OptimizedMatPool {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedMatPool.class);

    private static final int DEFAULT_POOL_SIZE = 16;

    private final BlockingQueue<Mat> pool;
    private final AtomicInteger hits = new AtomicInteger(0);
    private final AtomicInteger misses = new AtomicInteger(0);
    private final int maxPoolSize;

    /**
     * Creates a new Mat pool with default size.
     */
    public OptimizedMatPool() {
        this(DEFAULT_POOL_SIZE);
    }

    /**
     * Creates a new Mat pool with specified size.
     *
     * @param maxPoolSize maximum number of Mat objects to pool
     */
    public OptimizedMatPool(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        this.pool = new ArrayBlockingQueue<>(maxPoolSize);
    }

    /**
     * Acquires a Mat from the pool or creates a new one if pool is empty.
     *
     * @return a Mat object ready for use
     */
    public Mat acquire() {
        Mat mat = pool.poll();
        if (mat != null && !mat.isNull()) {
            hits.incrementAndGet();
            return mat;
        }
        misses.incrementAndGet();
        return new Mat();
    }

    /**
     * Returns a Mat to the pool for reuse.
     * The Mat is cleared before being returned to ensure clean state.
     *
     * @param mat the Mat to return to the pool
     */
    public void release(Mat mat) {
        if (mat == null || mat.isNull()) {
            return;
        }

        try {
            // Clear the Mat to avoid memory leaks from large image data
            if (!mat.empty()) {
                mat.create(0, 0, mat.type());
            }

            // Only return to pool if not full
            if (!pool.offer(mat)) {
                // Pool is full, release the Mat
                mat.releaseReference();
            }
        } catch (Exception e) {
            logger.debug("Error releasing Mat to pool: {}", e.getMessage());
            try {
                mat.releaseReference();
            } catch (Exception ignored) {
                // Best effort cleanup
            }
        }
    }

    /**
     * Gets the pool hit rate (percentage of requests served from pool).
     *
     * @return hit rate as a percentage (0-100)
     */
    public double getHitRate() {
        int totalHits = hits.get();
        int totalMisses = misses.get();
        int total = totalHits + totalMisses;

        if (total == 0) return 0.0;

        return (double) totalHits / total * 100.0;
    }

    /**
     * Gets the current number of available Mat objects in the pool.
     *
     * @return number of pooled Mat objects
     */
    public int getPoolSize() {
        return pool.size();
    }

    /**
     * Clears the pool and releases all Mat objects.
     */
    public void clear() {
        int released = 0;
        Mat mat;
        while ((mat = pool.poll()) != null) {
            try {
                mat.releaseReference();
                released++;
            } catch (Exception e) {
                logger.debug("Error releasing pooled Mat during clear: {}", e.getMessage());
            }
        }

        if (released > 0) {
            logger.debug("Cleared {} Mat objects from pool", released);
        }
    }

    /**
     * Logs pool statistics for monitoring purposes.
     */
    public void logStatistics() {
        int totalHits = hits.get();
        int totalMisses = misses.get();
        int total = totalHits + totalMisses;

        logger.info("Mat Pool Statistics:");
        logger.info("  Total requests: {}", total);
        logger.info("  Hits: {} ({:.2f}%)", totalHits, getHitRate());
        logger.info("  Misses: {}", totalMisses);
        logger.info("  Current pool size: {}/{}", getPoolSize(), maxPoolSize);
    }

    /**
     * Resets pool statistics counters.
     */
    public void resetStatistics() {
        hits.set(0);
        misses.set(0);
        logger.debug("Pool statistics reset");
    }

    /**
     * Gets the maximum pool size.
     *
     * @return maximum number of Mat objects that can be pooled
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * Pre-fills the pool with Mat objects for improved startup performance.
     *
     * @param count number of Mat objects to pre-create
     */
    public void preFill(int count) {
        int toCreate = Math.min(count, maxPoolSize - getPoolSize());

        for (int i = 0; i < toCreate; i++) {
            Mat mat = new Mat();
            if (!pool.offer(mat)) {
                // Pool is full, release the Mat
                mat.releaseReference();
                break;
            }
        }

        logger.debug("Pre-filled pool with {} Mat objects", toCreate);
    }

    /**
     * Acquires multiple Mat objects at once for batch operations.
     *
     * @param count number of Mat objects needed
     * @return list of Mat objects (may be less than requested if pool is empty)
     */
    public List<Mat> acquireBatch(int count) {
        List<Mat> mats = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            mats.add(acquire());
        }

        return mats;
    }

    /**
     * Releases multiple Mat objects back to the pool.
     *
     * @param mats list of Mat objects to return
     */
    public void releaseBatch(List<Mat> mats) {
        if (mats == null) return;

        for (Mat mat : mats) {
            release(mat);
        }
    }
}
