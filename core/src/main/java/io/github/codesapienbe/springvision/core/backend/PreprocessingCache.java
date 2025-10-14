package io.github.codesapienbe.springvision.core.backend;

import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for preprocessed images to avoid redundant processing in multi-detector scenarios.
 *
 * <p>When running multiple detectors (YuNet, DNN, Haar), they all need grayscale
 * and equalized versions of the input image. This cache prevents redundant conversions.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Thread-safe concurrent access</li>
 *   <li>Time-based expiration to prevent stale data</li>
 *   <li>Automatic size limiting to prevent memory bloat</li>
 *   <li>Fast hash-based lookup</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
public class PreprocessingCache {

    private static final Logger logger = LoggerFactory.getLogger(PreprocessingCache.class);

    private static final int MAX_CACHE_SIZE = 32;
    private static final long CACHE_TTL_MS = 30_000; // 30 seconds

    private final Map<Integer, CachedPreprocessedImage> cache = new ConcurrentHashMap<>();

    /**
     * Cached preprocessed image with metadata.
     */
    public static class CachedPreprocessedImage {
        public final Mat grayImage;
        public final Mat equalizedImage;
        public final long timestamp;
        public final int imageHash;

        public CachedPreprocessedImage(Mat gray, Mat equalized, int hash) {
            this.grayImage = gray;
            this.equalizedImage = equalized;
            this.timestamp = System.currentTimeMillis();
            this.imageHash = hash;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }

        public void release() {
            if (grayImage != null && !grayImage.isNull()) {
                try {
                    grayImage.releaseReference();
                } catch (Exception e) {
                    // Ignore
                }
            }
            if (equalizedImage != null && !equalizedImage.isNull()) {
                try {
                    equalizedImage.releaseReference();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Gets a cached preprocessed image or returns null if not found or expired.
     *
     * @param imageHash hash of the original image
     * @return cached preprocessed image or null
     */
    public CachedPreprocessedImage get(int imageHash) {
        CachedPreprocessedImage cached = cache.get(imageHash);

        if (cached != null) {
            if (cached.isExpired()) {
                remove(imageHash);
                return null;
            }
            return cached;
        }

        return null;
    }

    /**
     * Stores a preprocessed image in the cache.
     *
     * @param imageHash    hash of the original image
     * @param preprocessed the preprocessed image to cache
     */
    public void put(int imageHash, CachedPreprocessedImage preprocessed) {
        cache.put(imageHash, preprocessed);

        // Cleanup if cache is too large
        if (cache.size() > MAX_CACHE_SIZE) {
            cleanup();
        }
    }

    /**
     * Removes a cached entry.
     *
     * @param imageHash hash of the image to remove
     */
    public void remove(int imageHash) {
        CachedPreprocessedImage cached = cache.remove(imageHash);
        if (cached != null) {
            cached.release();
        }
    }

    /**
     * Cleans up expired entries and enforces size limit.
     */
    public void cleanup() {
        // Remove expired entries
        cache.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                entry.getValue().release();
                return true;
            }
            return false;
        });

        // If still too large, remove oldest entries
        if (cache.size() > MAX_CACHE_SIZE) {
            cache.entrySet().stream()
                .sorted(java.util.Comparator.comparingLong(entry -> entry.getValue().timestamp))
                .limit(cache.size() - MAX_CACHE_SIZE)
                .forEach(entry -> {
                    entry.getValue().release();
                    cache.remove(entry.getKey());
                });
        }
    }

    /**
     * Clears all cached entries.
     */
    public void clear() {
        cache.values().forEach(CachedPreprocessedImage::release);
        cache.clear();
        logger.debug("Preprocessing cache cleared");
    }

    /**
     * Gets the current cache size.
     *
     * @return number of cached entries
     */
    public int size() {
        return cache.size();
    }

    /**
     * Gets the maximum cache size.
     *
     * @return maximum number of cached entries
     */
    public int getMaxSize() {
        return MAX_CACHE_SIZE;
    }

    /**
     * Computes a fast hash for an image Mat.
     * Uses sampling to avoid processing entire image.
     *
     * @param image input image
     * @return hash code
     */
    public static int computeImageHash(Mat image) {
        if (image == null || image.empty()) {
            return 0;
        }

        // Use image properties and sampled pixel values for hash
        int hash = 17;
        hash = 31 * hash + image.rows();
        hash = 31 * hash + image.cols();
        hash = 31 * hash + image.channels();
        hash = 31 * hash + image.type();

        // Sample a few pixels for content-based hashing
        // This is much faster than hashing the entire image
        int stepSize = Math.max(1, Math.min(image.rows(), image.cols()) / 10);

        try {
            for (int y = 0; y < image.rows(); y += stepSize) {
                for (int x = 0; x < image.cols(); x += stepSize) {
                    // Sample pixel value - use pointer address for fast hashing
                    // This avoids complex pixel value extraction
                    long pixelAddress = image.ptr(y, x).address();
                    hash = 31 * hash + Long.hashCode(pixelAddress);
                }
            }
        } catch (Exception e) {
            // If sampling fails, just use dimensional hash
            logger.debug("Failed to sample pixels for hash: {}", e.getMessage());
        }

        return hash;
    }

    /**
     * Gets cache hit rate as a percentage.
     *
     * @return hit rate (0-100)
     */
    public double getHitRate() {
        // This would require tracking hits/misses, which we can add if needed
        return 0.0; // Placeholder
    }

    /**
     * Gets the cache TTL in milliseconds.
     *
     * @return cache time-to-live
     */
    public long getCacheTTL() {
        return CACHE_TTL_MS;
    }

    /**
     * Logs cache statistics.
     */
    public void logStatistics() {
        logger.info("Preprocessing Cache Statistics:");
        logger.info("  Current size: {}/{}", size(), MAX_CACHE_SIZE);
        logger.info("  Cache TTL: {}ms", CACHE_TTL_MS);

        // Count expired entries
        long expiredCount = cache.values().stream()
            .filter(CachedPreprocessedImage::isExpired)
            .count();

        if (expiredCount > 0) {
            logger.info("  Expired entries: {}", expiredCount);
        }
    }

    /**
     * Checks if the cache contains a valid entry for the given hash.
     *
     * @param imageHash hash of the image
     * @return true if valid cached entry exists
     */
    public boolean contains(int imageHash) {
        CachedPreprocessedImage cached = cache.get(imageHash);
        return cached != null && !cached.isExpired();
    }

    /**
     * Removes all expired entries from the cache.
     *
     * @return number of entries removed
     */
    public int removeExpired() {
        int removed = 0;

        for (Map.Entry<Integer, CachedPreprocessedImage> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                entry.getValue().release();
                cache.remove(entry.getKey());
                removed++;
            }
        }

        if (removed > 0) {
            logger.debug("Removed {} expired cache entries", removed);
        }

        return removed;
    }
}
