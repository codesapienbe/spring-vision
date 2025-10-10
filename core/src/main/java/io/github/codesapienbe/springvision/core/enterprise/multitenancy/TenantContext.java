package io.github.codesapienbe.springvision.core.enterprise.multitenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive multi-tenancy context management for the Spring Vision framework.
 *
 * <p>This class provides tenant isolation, resource management, and context propagation
 * across the entire application stack. It ensures complete separation between tenants
 * while maintaining performance and security.</p>
 *
 * <p>The tenant context supports dynamic tenant creation, resource quotas, and
 * comprehensive monitoring and analytics per tenant.</p>
 *
 * @author Spring Vision Team
 * @since 1.2.0
 */
@Component
public class TenantContext {

    private static final Logger logger = LoggerFactory.getLogger(TenantContext.class);

    // Thread-local tenant context
    private static final ThreadLocal<TenantInfo> currentTenant = new ThreadLocal<>();

    // Tenant registry with metadata
    private static final Map<String, TenantInfo> tenantRegistry = new ConcurrentHashMap<>();

    // Tenant metrics and quotas
    private static final Map<String, TenantMetrics> tenantMetrics = new ConcurrentHashMap<>();

    // Tenant resource limits
    private static final Map<String, TenantQuotas> tenantQuotas = new ConcurrentHashMap<>();

    // Default tenant configuration
    private static final String DEFAULT_TENANT_ID = "default";
    private static final TenantQuotas DEFAULT_QUOTAS = new TenantQuotas(
        1000, // max requests per hour
        100 * 1024 * 1024, // max image size (100MB)
        10, // max concurrent requests
        1000 * 1024 * 1024 // max memory usage (1GB)
    );

    /**
     * Sets the current tenant context.
     */
    public static void setCurrentTenant(String tenantId) {
        TenantInfo tenantInfo = getOrCreateTenant(tenantId);
        currentTenant.set(tenantInfo);

        logger.debug("Tenant context set: {}", tenantId);
    }

    /**
     * Gets the current tenant ID.
     */
    public static String getCurrentTenantId() {
        TenantInfo tenantInfo = currentTenant.get();
        return tenantInfo != null ? tenantInfo.getTenantId() : DEFAULT_TENANT_ID;
    }

    /**
     * Gets the current tenant information.
     */
    public static TenantInfo getCurrentTenant() {
        TenantInfo tenantInfo = currentTenant.get();
        return tenantInfo != null ? tenantInfo : getOrCreateTenant(DEFAULT_TENANT_ID);
    }

    /**
     * Clears the current tenant context.
     */
    public static void clearCurrentTenant() {
        currentTenant.remove();
        logger.debug("Tenant context cleared");
    }

    /**
     * Registers a new tenant with configuration.
     */
    public static void registerTenant(String tenantId, TenantConfiguration config) {
        TenantInfo tenantInfo = new TenantInfo(tenantId, config);
        tenantRegistry.put(tenantId, tenantInfo);

        // Initialize metrics and quotas
        tenantMetrics.put(tenantId, new TenantMetrics());
        tenantQuotas.put(tenantId, config.getQuotas());

        logger.info("Tenant registered: {} with configuration: {}", tenantId, config);
    }

    /**
     * Unregisters a tenant and cleans up resources.
     */
    public static void unregisterTenant(String tenantId) {
        tenantRegistry.remove(tenantId);
        tenantMetrics.remove(tenantId);
        tenantQuotas.remove(tenantId);

        logger.info("Tenant unregistered: {}", tenantId);
    }

    /**
     * Checks if a tenant exists.
     */
    public static boolean tenantExists(String tenantId) {
        return tenantRegistry.containsKey(tenantId);
    }

    /**
     * Gets tenant configuration.
     */
    public static TenantConfiguration getTenantConfiguration(String tenantId) {
        TenantInfo tenantInfo = tenantRegistry.get(tenantId);
        return tenantInfo != null ? tenantInfo.getConfiguration() : null;
    }

    /**
     * Updates tenant configuration.
     */
    public static void updateTenantConfiguration(String tenantId, TenantConfiguration config) {
        TenantInfo tenantInfo = tenantRegistry.get(tenantId);
        if (tenantInfo != null) {
            tenantInfo.setConfiguration(config);
            tenantQuotas.put(tenantId, config.getQuotas());
            logger.info("Tenant configuration updated: {}", tenantId);
        }
    }

    /**
     * Records a request for the current tenant.
     */
    public static void recordRequest(String operation, long processingTimeMs, int detectionCount) {
        String tenantId = getCurrentTenantId();
        TenantMetrics metrics = getTenantMetrics(tenantId);

        metrics.recordRequest(operation, processingTimeMs, detectionCount);

        logger.debug("Request recorded for tenant {}: operation={}, time={}ms, detections={}",
            tenantId, operation, processingTimeMs, detectionCount);
    }

    /**
     * Records an error for the current tenant.
     */
    public static void recordError(String operation, String errorType) {
        String tenantId = getCurrentTenantId();
        TenantMetrics metrics = getTenantMetrics(tenantId);

        metrics.recordError(operation, errorType);

        logger.warn("Error recorded for tenant {}: operation={}, error={}",
            tenantId, operation, errorType);
    }

    /**
     * Checks if the current tenant has exceeded quotas.
     */
    public static boolean checkQuotas(String operation, long imageSizeBytes) {
        String tenantId = getCurrentTenantId();
        TenantQuotas quotas = getTenantQuotas(tenantId);
        TenantMetrics metrics = getTenantMetrics(tenantId);

        // Check request rate limit
        if (metrics.getRequestsThisHour() >= quotas.getMaxRequestsPerHour()) {
            logger.warn("Tenant {} exceeded request rate limit", tenantId);
            return false;
        }

        // Check concurrent requests
        if (metrics.getActiveRequests() >= quotas.getMaxConcurrentRequests()) {
            logger.warn("Tenant {} exceeded concurrent request limit", tenantId);
            return false;
        }

        // Check image size limit
        if (imageSizeBytes > quotas.getMaxImageSize()) {
            logger.warn("Tenant {} exceeded image size limit: {} bytes", tenantId, imageSizeBytes);
            return false;
        }

        // Check memory usage
        if (metrics.getMemoryUsage() > quotas.getMaxMemoryUsage()) {
            logger.warn("Tenant {} exceeded memory usage limit", tenantId);
            return false;
        }

        return true;
    }

    /**
     * Gets tenant metrics.
     */
    public static TenantMetrics getTenantMetrics(String tenantId) {
        return tenantMetrics.computeIfAbsent(tenantId, k -> new TenantMetrics());
    }

    /**
     * Gets tenant quotas.
     */
    public static TenantQuotas getTenantQuotas(String tenantId) {
        return tenantQuotas.getOrDefault(tenantId, DEFAULT_QUOTAS);
    }

    /**
     * Gets all tenant metrics.
     */
    public static Map<String, TenantMetrics> getAllTenantMetrics() {
        return new ConcurrentHashMap<>(tenantMetrics);
    }

    /**
     * Gets all registered tenants.
     */
    public static Map<String, TenantInfo> getAllTenants() {
        return new ConcurrentHashMap<>(tenantRegistry);
    }

    /**
     * Resets metrics for a tenant.
     */
    public static void resetTenantMetrics(String tenantId) {
        TenantMetrics metrics = tenantMetrics.get(tenantId);
        if (metrics != null) {
            metrics.reset();
            logger.info("Metrics reset for tenant: {}", tenantId);
        }
    }

    /**
     * Gets or creates a tenant with default configuration.
     */
    private static TenantInfo getOrCreateTenant(String tenantId) {
        return tenantRegistry.computeIfAbsent(tenantId, k -> {
            TenantConfiguration defaultConfig = new TenantConfiguration(
                DEFAULT_QUOTAS,
                true, // enabled
                "default", // tier
                Map.of() // custom properties
            );

            TenantInfo tenantInfo = new TenantInfo(tenantId, defaultConfig);
            tenantMetrics.put(tenantId, new TenantMetrics());
            tenantQuotas.put(tenantId, DEFAULT_QUOTAS);

            logger.info("Auto-created tenant: {}", tenantId);
            return tenantInfo;
        });
    }

    /**
     * Tenant information data class.
     */
    public static class TenantInfo {
        private final String tenantId;
        private TenantConfiguration configuration;
        private final long createdAt;
        private long lastAccessedAt;

        public TenantInfo(String tenantId, TenantConfiguration configuration) {
            this.tenantId = tenantId;
            this.configuration = configuration;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessedAt = System.currentTimeMillis();
        }

        public void updateLastAccessed() {
            this.lastAccessedAt = System.currentTimeMillis();
        }

        // Getters and setters
        public String getTenantId() {
            return tenantId;
        }

        public TenantConfiguration getConfiguration() {
            return configuration;
        }

        public void setConfiguration(TenantConfiguration configuration) {
            this.configuration = configuration;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getLastAccessedAt() {
            return lastAccessedAt;
        }
    }

    /**
     * Tenant configuration data class.
     */
    public static class TenantConfiguration {
        private final TenantQuotas quotas;
        private final boolean enabled;
        private final String tier;
        private final Map<String, Object> customProperties;

        public TenantConfiguration(TenantQuotas quotas, boolean enabled, String tier, Map<String, Object> customProperties) {
            this.quotas = quotas;
            this.enabled = enabled;
            this.tier = tier;
            this.customProperties = customProperties;
        }

        // Getters
        public TenantQuotas getQuotas() {
            return quotas;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getTier() {
            return tier;
        }

        public Map<String, Object> getCustomProperties() {
            return customProperties;
        }
    }

    /**
     * Tenant quotas data class.
     */
    public static class TenantQuotas {
        private final int maxRequestsPerHour;
        private final long maxImageSize;
        private final int maxConcurrentRequests;
        private final long maxMemoryUsage;

        public TenantQuotas(int maxRequestsPerHour, long maxImageSize, int maxConcurrentRequests, long maxMemoryUsage) {
            this.maxRequestsPerHour = maxRequestsPerHour;
            this.maxImageSize = maxImageSize;
            this.maxConcurrentRequests = maxConcurrentRequests;
            this.maxMemoryUsage = maxMemoryUsage;
        }

        // Getters
        public int getMaxRequestsPerHour() {
            return maxRequestsPerHour;
        }

        public long getMaxImageSize() {
            return maxImageSize;
        }

        public int getMaxConcurrentRequests() {
            return maxConcurrentRequests;
        }

        public long getMaxMemoryUsage() {
            return maxMemoryUsage;
        }
    }

    /**
     * Tenant metrics data class.
     */
    public static class TenantMetrics {
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong totalErrors = new AtomicLong(0);
        private final AtomicLong totalProcessingTime = new AtomicLong(0);
        private final AtomicLong totalDetections = new AtomicLong(0);
        private final AtomicLong activeRequests = new AtomicLong(0);
        private final AtomicLong memoryUsage = new AtomicLong(0);
        private final AtomicLong requestsThisHour = new AtomicLong(0);
        private final Map<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
        private long lastResetTime = System.currentTimeMillis();

        public void recordRequest(String operation, long processingTimeMs, int detectionCount) {
            totalRequests.incrementAndGet();
            totalProcessingTime.addAndGet(processingTimeMs);
            totalDetections.addAndGet(detectionCount);
            requestsThisHour.incrementAndGet();

            operationCounts.computeIfAbsent(operation, k -> new AtomicLong()).incrementAndGet();

            // Reset hourly counter if needed
            long now = System.currentTimeMillis();
            if (now - lastResetTime > 3600000) { // 1 hour
                requestsThisHour.set(0);
                lastResetTime = now;
            }
        }

        public void recordError(String operation, String errorType) {
            totalErrors.incrementAndGet();
            errorCounts.computeIfAbsent(errorType, k -> new AtomicLong()).incrementAndGet();
        }

        public void incrementActiveRequests() {
            activeRequests.incrementAndGet();
        }

        public void decrementActiveRequests() {
            activeRequests.decrementAndGet();
        }

        public void setMemoryUsage(long memoryUsage) {
            this.memoryUsage.set(memoryUsage);
        }

        public void reset() {
            totalRequests.set(0);
            totalErrors.set(0);
            totalProcessingTime.set(0);
            totalDetections.set(0);
            activeRequests.set(0);
            memoryUsage.set(0);
            requestsThisHour.set(0);
            operationCounts.clear();
            errorCounts.clear();
            lastResetTime = System.currentTimeMillis();
        }

        // Getters
        public long getTotalRequests() {
            return totalRequests.get();
        }

        public long getTotalErrors() {
            return totalErrors.get();
        }

        public long getTotalProcessingTime() {
            return totalProcessingTime.get();
        }

        public long getTotalDetections() {
            return totalDetections.get();
        }

        public long getActiveRequests() {
            return activeRequests.get();
        }

        public long getMemoryUsage() {
            return memoryUsage.get();
        }

        public long getRequestsThisHour() {
            return requestsThisHour.get();
        }

        public Map<String, Long> getOperationCounts() {
            Map<String, Long> result = new ConcurrentHashMap<>();
            operationCounts.forEach((k, v) -> result.put(k, v.get()));
            return result;
        }

        public Map<String, Long> getErrorCounts() {
            Map<String, Long> result = new ConcurrentHashMap<>();
            errorCounts.forEach((k, v) -> result.put(k, v.get()));
            return result;
        }

        public double getErrorRate() {
            long total = totalRequests.get();
            return total > 0 ? (double) totalErrors.get() / total : 0.0;
        }

        public double getAverageProcessingTime() {
            long total = totalRequests.get();
            return total > 0 ? (double) totalProcessingTime.get() / total : 0.0;
        }
    }
}
