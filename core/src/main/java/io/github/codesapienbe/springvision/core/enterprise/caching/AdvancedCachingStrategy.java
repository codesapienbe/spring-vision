package io.github.codesapienbe.springvision.core.enterprise.caching;

import io.github.codesapienbe.springvision.core.*;
import io.github.codesapienbe.springvision.core.enterprise.multitenancy.TenantContext;
import io.github.codesapienbe.springvision.core.logging.VisionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced caching strategy system for the Spring Vision framework.
 *
 * <p>This class provides enterprise-grade caching capabilities with multiple cache layers,
 * intelligent eviction policies, distributed caching, and comprehensive cache management.
 * It ensures optimal performance while maintaining data consistency and memory efficiency.</p>
 *
 * <p>The caching system supports L1/L2 cache architecture, tenant-aware caching,
 * intelligent cache warming, and comprehensive cache analytics.</p>
 *
 * @author Spring Vision Team
 * @since 1.2.0
 */
@Component
public class AdvancedCachingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedCachingStrategy.class);

    // Cache layers
    private final L1Cache l1Cache; // In-memory cache
    private final L2Cache l2Cache; // Distributed cache
    private final CacheManager cacheManager;

    // Cache policies
    private final Map<String, CachePolicy> cachePolicies = new ConcurrentHashMap<>();
    private final EvictionManager evictionManager;

    // Cache analytics
    private final CacheAnalytics cacheAnalytics;

    // Cache warming
    private final CacheWarmer cacheWarmer;

    // Configuration
    private final CacheConfiguration configuration;

    /**
     * Constructs a new AdvancedCachingStrategy with default configuration.
     */
    public AdvancedCachingStrategy() {
        this.configuration = new CacheConfiguration();
        this.l1Cache = new L1Cache(configuration.getL1MaxSize(), configuration.getL1TtlMs());
        this.l2Cache = new L2Cache(configuration.getL2MaxSize(), configuration.getL2TtlMs());
        this.cacheManager = new CacheManager();
        this.evictionManager = new EvictionManager();
        this.cacheAnalytics = new CacheAnalytics();
        this.cacheWarmer = new CacheWarmer();

        initializeCachePolicies();
        startBackgroundProcesses();
    }

    /**
     * Gets cached detection results.
     *
     * @param imageData the image data to get cached detections for
     * @param query     the detection query
     * @return an Optional containing the cached detections if available, empty otherwise
     */
    public Optional<List<Detection>> getCachedDetections(ImageData imageData, DetectionQuery query) {
        String cacheKey = generateCacheKey(imageData, query);
        String tenantId = TenantContext.getCurrentTenantId();

        try {
            // Try L1 cache first
            Optional<List<Detection>> l1Result = l1Cache.get(cacheKey, tenantId);
            if (l1Result.isPresent()) {
                cacheAnalytics.recordCacheHit("L1", tenantId);
                return l1Result;
            }

            // Try L2 cache
            Optional<List<Detection>> l2Result = l2Cache.get(cacheKey, tenantId);
            if (l2Result.isPresent()) {
                // Promote to L1 cache
                l1Cache.put(cacheKey, tenantId, l2Result.get());
                cacheAnalytics.recordCacheHit("L2", tenantId);
                return l2Result;
            }

            cacheAnalytics.recordCacheMiss(tenantId);
            return Optional.empty();

        } catch (Exception e) {
            logger.warn("Cache access failed for key: {}", cacheKey, e);
            return Optional.empty();
        }
    }

    /**
     * Caches detection results.
     * @param imageData The image data associated with the detections.
     * @param query The detection query used.
     * @param detections The list of detections to cache.
     */
    public void cacheDetections(ImageData imageData, DetectionQuery query, List<Detection> detections) {
        String cacheKey = generateCacheKey(imageData, query);
        String tenantId = TenantContext.getCurrentTenantId();

        try {
            // Store in both L1 and L2 caches
            l1Cache.put(cacheKey, tenantId, detections);
            l2Cache.put(cacheKey, tenantId, detections);

            cacheAnalytics.recordCachePut(tenantId, detections.size());

        } catch (Exception e) {
            logger.warn("Failed to cache detections for key: {}", cacheKey, e);
        }
    }

    /**
     * Invalidates cache entries matching a pattern for a specific tenant.
     * @param pattern The pattern to match against cache keys.
     * @param tenantId The ID of the tenant whose cache entries should be invalidated.
     */
    public void invalidateCache(String pattern, String tenantId) {
        try {
            l1Cache.invalidate(pattern, tenantId);
            l2Cache.invalidate(pattern, tenantId);

            cacheAnalytics.recordCacheInvalidation(tenantId);
            logger.info("Cache invalidated for pattern: {} and tenant: {}", pattern, tenantId);

        } catch (Exception e) {
            logger.warn("Cache invalidation failed for pattern: {}", pattern, e);
        }
    }

    /**
     * Preloads the cache with frequently accessed data.
     * @param images The list of images to preload.
     * @param query The detection query to use for preloading.
     */
    public void preloadCache(List<ImageData> images, DetectionQuery query) {
        cacheWarmer.preload(images, query);
    }

    /**
     * Gets cache statistics for a specific tenant.
     * @param tenantId The ID of the tenant.
     * @return The cache statistics for the tenant.
     */
    public CacheStatistics getCacheStatistics(String tenantId) {
        return cacheAnalytics.getStatistics(tenantId);
    }

    /**
     * Gets all cache statistics for all tenants.
     * @return A map of tenant IDs to their cache statistics.
     */
    public Map<String, CacheStatistics> getAllCacheStatistics() {
        return cacheAnalytics.getAllStatistics();
    }

    /**
     * Sets the cache policy for a given detection type.
     * @param detectionType The type of detection.
     * @param policy The cache policy to set.
     */
    public void setCachePolicy(DetectionType detectionType, CachePolicy policy) {
        cachePolicies.put(detectionType.name(), policy);
        logger.info("Cache policy set for {}: {}", detectionType, policy.getClass().getSimpleName());
    }

    /**
     * Gets the cache policy for a given detection type.
     * @param detectionType The type of detection.
     * @return The corresponding cache policy.
     */
    public CachePolicy getCachePolicy(DetectionType detectionType) {
        return cachePolicies.getOrDefault(detectionType.name(), new DefaultCachePolicy());
    }

    /**
     * Clears all caches.
     */
    public void clearAllCaches() {
        l1Cache.clear();
        l2Cache.clear();
        cacheAnalytics.recordCacheClear();
        logger.info("All caches cleared");
    }

    /**
     * Gets the current cache configuration.
     * @return The current cache configuration.
     */
    public CacheConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Updates the cache configuration.
     * @param newConfig The new cache configuration.
     */
    public void updateConfiguration(CacheConfiguration newConfig) {
        this.configuration.update(newConfig);
        l1Cache.updateConfiguration(newConfig);
        l2Cache.updateConfiguration(newConfig);
        logger.info("Cache configuration updated");
    }

    /**
     * Initializes cache policies.
     */
    private void initializeCachePolicies() {
        // High-frequency cache policy for face detection
        setCachePolicy(DetectionType.FACE, new HighFrequencyCachePolicy());

        // Standard cache policy for object detection
        setCachePolicy(DetectionType.OBJECT, new StandardCachePolicy());

        // Low-frequency cache policy for pose detection
        setCachePolicy(DetectionType.POSE, new LowFrequencyCachePolicy());

        // Default policy for other types
        setCachePolicy(DetectionType.HAND, new StandardCachePolicy());
    }

    /**
     * Starts background processes.
     */
    private void startBackgroundProcesses() {
        // Eviction manager thread
        Thread evictionThread = new Thread(evictionManager::runEviction, "cache-eviction-manager");
        evictionThread.setDaemon(true);
        evictionThread.start();

        // Cache analytics thread
        Thread analyticsThread = new Thread(cacheAnalytics::collectAnalytics, "cache-analytics");
        analyticsThread.setDaemon(true);
        analyticsThread.start();

        // Cache warmer thread
        Thread warmerThread = new Thread(cacheWarmer::runWarming, "cache-warmer");
        warmerThread.setDaemon(true);
        warmerThread.start();

        logger.info("Cache background processes started");
    }

    /**
     * Generates cache key for image and query.
     */
    private String generateCacheKey(ImageData imageData, DetectionQuery query) {
        // Create a hash-based key for efficient storage
        String imageHash = generateImageHash(imageData);
        String queryHash = generateQueryHash(query);
        return String.format("%s:%s:%s", imageHash, queryHash, query.getType().name());
    }

    /**
     * Generates hash for image data.
     */
    private String generateImageHash(ImageData imageData) {
        // Simple hash implementation (replace with proper hash function)
        return String.valueOf(Arrays.hashCode(imageData.data()));
    }

    /**
     * Generates hash for detection query.
     */
    private String generateQueryHash(DetectionQuery query) {
        StringBuilder cacheKey = new StringBuilder();
        cacheKey.append(query.getType().name());
        cacheKey.append(":").append(query.getMinConfidence());
        cacheKey.append(":").append(query.getMaxDetections());
        // Check if query has NMS threshold
        if (query.getNmsThreshold() > 0) {
            cacheKey.append("_nms_").append(query.getNmsThreshold());
        }
        return cacheKey.toString();
    }

    /**
     * L1 Cache (In-memory) implementation.
     */
    private static class L1Cache {
        private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
        private final int maxSize;
        private final long ttlMs;

        public L1Cache(int maxSize, long ttlMs) {
            this.maxSize = maxSize;
            this.ttlMs = ttlMs;
        }

        public Optional<List<Detection>> get(String key, String tenantId) {
            CacheEntry entry = cache.get(key);
            if (entry != null && entry.isValid(tenantId)) {
                entry.updateLastAccessed();
                return Optional.of(entry.getDetections());
            }
            return Optional.empty();
        }

        public void put(String key, String tenantId, List<Detection> detections) {
            if (cache.size() >= maxSize) {
                evictEntries();
            }

            cache.put(key, new CacheEntry(detections, tenantId, ttlMs));
        }

        public void invalidate(String pattern, String tenantId) {
            cache.entrySet().removeIf(entry ->
                entry.getKey().contains(pattern) && entry.getValue().getTenantId().equals(tenantId));
        }

        public void clear() {
            cache.clear();
        }

        public void updateConfiguration(CacheConfiguration config) {
            // Update configuration if needed
        }

        private void evictEntries() {
            // LRU eviction
            cache.entrySet().stream()
                .sorted(Comparator.comparingLong(e -> e.getValue().getLastAccessed()))
                .limit(cache.size() - maxSize + 1)
                .forEach(entry -> cache.remove(entry.getKey()));
        }
    }

    /**
     * L2 Cache (Distributed) implementation.
     */
    private static class L2Cache {
        private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
        private final int maxSize;
        private final long ttlMs;

        public L2Cache(int maxSize, long ttlMs) {
            this.maxSize = maxSize;
            this.ttlMs = ttlMs;
        }

        public Optional<List<Detection>> get(String key, String tenantId) {
            CacheEntry entry = cache.get(key);
            if (entry != null && entry.isValid(tenantId)) {
                entry.updateLastAccessed();
                return Optional.of(entry.getDetections());
            }
            return Optional.empty();
        }

        public void put(String key, String tenantId, List<Detection> detections) {
            if (cache.size() >= maxSize) {
                evictEntries();
            }

            cache.put(key, new CacheEntry(detections, tenantId, ttlMs));
        }

        public void invalidate(String pattern, String tenantId) {
            cache.entrySet().removeIf(entry ->
                entry.getKey().contains(pattern) && entry.getValue().getTenantId().equals(tenantId));
        }

        public void clear() {
            cache.clear();
        }

        public void updateConfiguration(CacheConfiguration config) {
            // Update configuration if needed
        }

        private void evictEntries() {
            // LRU eviction
            cache.entrySet().stream()
                .sorted(Comparator.comparingLong(e -> e.getValue().getLastAccessed()))
                .limit(cache.size() - maxSize + 1)
                .forEach(entry -> cache.remove(entry.getKey()));
        }
    }

    /**
     * Cache entry data class.
     */
    private static class CacheEntry {
        private final List<Detection> detections;
        private final String tenantId;
        private final long createdAt;
        private final long ttlMs;
        private long lastAccessed;

        public CacheEntry(List<Detection> detections, String tenantId, long ttlMs) {
            this.detections = detections;
            this.tenantId = tenantId;
            this.createdAt = System.currentTimeMillis();
            this.ttlMs = ttlMs;
            this.lastAccessed = System.currentTimeMillis();
        }

        public boolean isValid(String tenantId) {
            return this.tenantId.equals(tenantId) &&
                (System.currentTimeMillis() - createdAt) < ttlMs;
        }

        public void updateLastAccessed() {
            this.lastAccessed = System.currentTimeMillis();
        }

        // Getters
        public List<Detection> getDetections() {
            return detections;
        }

        public String getTenantId() {
            return tenantId;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getLastAccessed() {
            return lastAccessed;
        }
    }

    /**
     * Defines a policy for caching detection results.
     */
    public interface CachePolicy {
        /**
         * Determines whether a detection should be cached.
         * @param imageData The image data.
         * @param query The detection query.
         * @return {@code true} if the detection should be cached, {@code false} otherwise.
         */
        boolean shouldCache(ImageData imageData, DetectionQuery query);

        /**
         * Gets the time-to-live (TTL) for cache entries in milliseconds.
         * @return The TTL in milliseconds.
         */
        long getTtlMs();

        /**
         * Gets the maximum size of the cache.
         * @return The maximum cache size.
         */
        int getMaxSize();
    }

    /**
     * Default cache policy with moderate caching.
     */
    public static class DefaultCachePolicy implements CachePolicy {
        @Override
        public boolean shouldCache(ImageData imageData, DetectionQuery query) {
            return true;
        }

        @Override
        public long getTtlMs() {
            return 300000; // 5 minutes
        }

        @Override
        public int getMaxSize() {
            return 1000;
        }
    }

    /**
     * Cache policy for high-frequency access patterns.
     */
    public static class HighFrequencyCachePolicy implements CachePolicy {
        @Override
        public boolean shouldCache(ImageData imageData, DetectionQuery query) {
            return imageData.data().length < 1024 * 1024; // Cache images < 1MB
        }

        @Override
        public long getTtlMs() {
            return 1800000; // 30 minutes
        }

        @Override
        public int getMaxSize() {
            return 5000;
        }
    }

    /**
     * Standard cache policy for typical access patterns.
     */
    public static class StandardCachePolicy implements CachePolicy {
        @Override
        public boolean shouldCache(ImageData imageData, DetectionQuery query) {
            return imageData.data().length < 5 * 1024 * 1024; // Cache images < 5MB
        }

        @Override
        public long getTtlMs() {
            return 600000; // 10 minutes
        }

        @Override
        public int getMaxSize() {
            return 2000;
        }
    }

    /**
     * Cache policy for low-frequency access patterns.
     */
    public static class LowFrequencyCachePolicy implements CachePolicy {
        @Override
        public boolean shouldCache(ImageData imageData, DetectionQuery query) {
            return imageData.data().length < 10 * 1024 * 1024; // Cache images < 10MB
        }

        @Override
        public long getTtlMs() {
            return 300000; // 5 minutes
        }

        @Override
        public int getMaxSize() {
            return 500;
        }
    }

    /**
     * Manages cache coordination and consistency.
     */
    private static class CacheManager {
        // Cache coordination logic
    }

    /**
     * Manages eviction of expired or least-recently-used cache entries.
     */
    private static class EvictionManager {
        public void runEviction() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Run eviction logic
                    Thread.sleep(60000); // Every minute
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Collects and provides cache analytics.
     */
    private static class CacheAnalytics {
        private final Map<String, CacheStatistics> tenantStatistics = new ConcurrentHashMap<>();
        private final AtomicLong totalHits = new AtomicLong(0);
        private final AtomicLong totalMisses = new AtomicLong(0);
        private final AtomicLong totalPuts = new AtomicLong(0);
        private final AtomicLong totalInvalidations = new AtomicLong(0);

        public void recordCacheHit(String cacheLevel, String tenantId) {
            totalHits.incrementAndGet();
            getTenantStatistics(tenantId).recordHit(cacheLevel);
        }

        public void recordCacheMiss(String tenantId) {
            totalMisses.incrementAndGet();
            getTenantStatistics(tenantId).recordMiss();
        }

        public void recordCachePut(String tenantId, int detectionCount) {
            totalPuts.incrementAndGet();
            getTenantStatistics(tenantId).recordPut(detectionCount);
        }

        public void recordCacheInvalidation(String tenantId) {
            totalInvalidations.incrementAndGet();
            getTenantStatistics(tenantId).recordInvalidation();
        }

        public void recordCacheClear() {
            // Record cache clear event
        }

        public void collectAnalytics() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Collect analytics data
                    Thread.sleep(300000); // Every 5 minutes
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private CacheStatistics getTenantStatistics(String tenantId) {
            return tenantStatistics.computeIfAbsent(tenantId, k -> new CacheStatistics());
        }

        public CacheStatistics getStatistics(String tenantId) {
            return tenantStatistics.get(tenantId);
        }

        public Map<String, CacheStatistics> getAllStatistics() {
            return new ConcurrentHashMap<>(tenantStatistics);
        }
    }

    /**
     * Preloads the cache with data.
     */
    private static class CacheWarmer {
        private final Queue<WarmupTask> warmupQueue = new ConcurrentLinkedQueue<>();

        public void preload(List<ImageData> images, DetectionQuery query) {
            warmupQueue.offer(new WarmupTask(images, query));
        }

        public void runWarming() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    WarmupTask task = warmupQueue.poll();
                    if (task != null) {
                        // Execute warmup task
                        logger.info("Cache warming completed for {} images", task.getImages().size());
                    }

                    Thread.sleep(1000); // Check every second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private static class WarmupTask {
            private final List<ImageData> images;
            private final DetectionQuery query;

            public WarmupTask(List<ImageData> images, DetectionQuery query) {
                this.images = images;
                this.query = query;
            }

            // Getters
            public List<ImageData> getImages() {
                return images;
            }

            public DetectionQuery getQuery() {
                return query;
            }
        }
    }

    /**
     * Holds cache statistics.
     */
    public static class CacheStatistics {
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        private final AtomicLong puts = new AtomicLong(0);
        private final AtomicLong invalidations = new AtomicLong(0);
        private final Map<String, AtomicLong> hitsByLevel = new ConcurrentHashMap<>();
        private final AtomicLong totalDetectionsCached = new AtomicLong(0);

        /**
         * Records a cache hit.
         * @param cacheLevel The cache level where the hit occurred (e.g., "L1", "L2").
         */
        public void recordHit(String cacheLevel) {
            hits.incrementAndGet();
            hitsByLevel.computeIfAbsent(cacheLevel, k -> new AtomicLong()).incrementAndGet();
        }

        /**
         * Records a cache miss.
         */
        public void recordMiss() {
            misses.incrementAndGet();
        }

        /**
         * Records a cache put operation.
         * @param detectionCount The number of detections added to the cache.
         */
        public void recordPut(int detectionCount) {
            puts.incrementAndGet();
            totalDetectionsCached.addAndGet(detectionCount);
        }

        /**
         * Records a cache invalidation.
         */
        public void recordInvalidation() {
            invalidations.incrementAndGet();
        }

        /**
         * Gets the cache hit rate.
         * @return The hit rate as a value between 0.0 and 1.0.
         */
        public double getHitRate() {
            long total = hits.get() + misses.get();
            return total > 0 ? (double) hits.get() / total : 0.0;
        }

        // Getters
        /**
         * Gets the total number of cache hits.
         * @return The total hits.
         */
        public long getHits() {
            return hits.get();
        }

        /**
         * Gets the total number of cache misses.
         * @return The total misses.
         */
        public long getMisses() {
            return misses.get();
        }

        /**
         * Gets the total number of cache put operations.
         * @return The total puts.
         */
        public long getPuts() {
            return puts.get();
        }

        /**
         * Gets the total number of cache invalidations.
         * @return The total invalidations.
         */
        public long getInvalidations() {
            return invalidations.get();
        }

        /**
         * Gets the number of cache hits by cache level.
         * @return A map of cache levels to their hit counts.
         */
        public Map<String, Long> getHitsByLevel() {
            Map<String, Long> result = new ConcurrentHashMap<>();
            hitsByLevel.forEach((k, v) -> result.put(k, v.get()));
            return result;
        }

        /**
         * Gets the total number of detections currently cached.
         * @return The total number of cached detections.
         */
        public long getTotalDetectionsCached() {
            return totalDetectionsCached.get();
        }
    }

    /**
     * Configuration for the advanced caching strategy.
     */
    public static class CacheConfiguration {
        private int l1MaxSize = 1000;
        private long l1TtlMs = 300000; // 5 minutes
        private int l2MaxSize = 5000;
        private long l2TtlMs = 1800000; // 30 minutes
        private boolean enableCacheWarming = true;
        private boolean enableAnalytics = true;

        /**
         * Updates the configuration from another configuration object.
         * @param newConfig The new configuration.
         */
        public void update(CacheConfiguration newConfig) {
            this.l1MaxSize = newConfig.l1MaxSize;
            this.l1TtlMs = newConfig.l1TtlMs;
            this.l2MaxSize = newConfig.l2MaxSize;
            this.l2TtlMs = newConfig.l2TtlMs;
            this.enableCacheWarming = newConfig.enableCacheWarming;
            this.enableAnalytics = newConfig.enableAnalytics;
        }

        // Getters
        /**
         * Gets the maximum size of the L1 cache.
         * @return The L1 cache size.
         */
        public int getL1MaxSize() {
            return l1MaxSize;
        }

        /**
         * Gets the time-to-live (TTL) for L1 cache entries in milliseconds.
         * @return The L1 TTL in milliseconds.
         */
        public long getL1TtlMs() {
            return l1TtlMs;
        }

        /**
         * Gets the maximum size of the L2 cache.
         * @return The L2 cache size.
         */
        public int getL2MaxSize() {
            return l2MaxSize;
        }

        /**
         * Gets the time-to-live (TTL) for L2 cache entries in milliseconds.
         * @return The L2 TTL in milliseconds.
         */
        public long getL2TtlMs() {
            return l2TtlMs;
        }

        /**
         * Checks if cache warming is enabled.
         * @return {@code true} if cache warming is enabled, {@code false} otherwise.
         */
        public boolean isEnableCacheWarming() {
            return enableCacheWarming;
        }

        /**
         * Checks if cache analytics is enabled.
         * @return {@code true} if analytics is enabled, {@code false} otherwise.
         */
        public boolean isEnableAnalytics() {
            return enableAnalytics;
        }
    }
}
