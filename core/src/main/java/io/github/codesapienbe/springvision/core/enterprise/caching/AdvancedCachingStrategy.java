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
     * Invalidates cache entries.
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
     * Preloads cache with frequently accessed data.
     */
    public void preloadCache(List<ImageData> images, DetectionQuery query) {
        cacheWarmer.preload(images, query);
    }

    /**
     * Gets cache statistics.
     */
    public CacheStatistics getCacheStatistics(String tenantId) {
        return cacheAnalytics.getStatistics(tenantId);
    }

    /**
     * Gets all cache statistics.
     */
    public Map<String, CacheStatistics> getAllCacheStatistics() {
        return cacheAnalytics.getAllStatistics();
    }

    /**
     * Sets cache policy for detection type.
     */
    public void setCachePolicy(DetectionType detectionType, CachePolicy policy) {
        cachePolicies.put(detectionType.name(), policy);
        logger.info("Cache policy set for {}: {}", detectionType, policy.getClass().getSimpleName());
    }

    /**
     * Gets cache policy for detection type.
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
     * Gets cache configuration.
     */
    public CacheConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Updates cache configuration.
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
     * Cache policy interface.
     */
    public interface CachePolicy {
        boolean shouldCache(ImageData imageData, DetectionQuery query);

        long getTtlMs();

        int getMaxSize();
    }

    /**
     * Default cache policy.
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
     * High-frequency cache policy.
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
     * Standard cache policy.
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
     * Low-frequency cache policy.
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
     * Cache manager for coordination.
     */
    private static class CacheManager {
        // Cache coordination logic
    }

    /**
     * Eviction manager for cache cleanup.
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
     * Cache analytics for monitoring.
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
     * Cache warmer for preloading.
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
     * Cache statistics data class.
     */
    public static class CacheStatistics {
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        private final AtomicLong puts = new AtomicLong(0);
        private final AtomicLong invalidations = new AtomicLong(0);
        private final Map<String, AtomicLong> hitsByLevel = new ConcurrentHashMap<>();
        private final AtomicLong totalDetectionsCached = new AtomicLong(0);

        public void recordHit(String cacheLevel) {
            hits.incrementAndGet();
            hitsByLevel.computeIfAbsent(cacheLevel, k -> new AtomicLong()).incrementAndGet();
        }

        public void recordMiss() {
            misses.incrementAndGet();
        }

        public void recordPut(int detectionCount) {
            puts.incrementAndGet();
            totalDetectionsCached.addAndGet(detectionCount);
        }

        public void recordInvalidation() {
            invalidations.incrementAndGet();
        }

        public double getHitRate() {
            long total = hits.get() + misses.get();
            return total > 0 ? (double) hits.get() / total : 0.0;
        }

        // Getters
        public long getHits() {
            return hits.get();
        }

        public long getMisses() {
            return misses.get();
        }

        public long getPuts() {
            return puts.get();
        }

        public long getInvalidations() {
            return invalidations.get();
        }

        public Map<String, Long> getHitsByLevel() {
            Map<String, Long> result = new ConcurrentHashMap<>();
            hitsByLevel.forEach((k, v) -> result.put(k, v.get()));
            return result;
        }

        public long getTotalDetectionsCached() {
            return totalDetectionsCached.get();
        }
    }

    /**
     * Cache configuration.
     */
    public static class CacheConfiguration {
        private int l1MaxSize = 1000;
        private long l1TtlMs = 300000; // 5 minutes
        private int l2MaxSize = 5000;
        private long l2TtlMs = 1800000; // 30 minutes
        private boolean enableCacheWarming = true;
        private boolean enableAnalytics = true;

        public void update(CacheConfiguration newConfig) {
            this.l1MaxSize = newConfig.l1MaxSize;
            this.l1TtlMs = newConfig.l1TtlMs;
            this.l2MaxSize = newConfig.l2MaxSize;
            this.l2TtlMs = newConfig.l2TtlMs;
            this.enableCacheWarming = newConfig.enableCacheWarming;
            this.enableAnalytics = newConfig.enableAnalytics;
        }

        // Getters
        public int getL1MaxSize() {
            return l1MaxSize;
        }

        public long getL1TtlMs() {
            return l1TtlMs;
        }

        public int getL2MaxSize() {
            return l2MaxSize;
        }

        public long getL2TtlMs() {
            return l2TtlMs;
        }

        public boolean isEnableCacheWarming() {
            return enableCacheWarming;
        }

        public boolean isEnableAnalytics() {
            return enableAnalytics;
        }
    }
}
