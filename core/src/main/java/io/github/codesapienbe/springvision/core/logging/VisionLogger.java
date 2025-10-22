package io.github.codesapienbe.springvision.core.logging;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.DetectionQuery;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive logging utility for the Spring Vision framework.
 *
 * <p>This class provides structured logging capabilities with correlation IDs,
 * performance metrics, and standardized log formats across all vision backends.
 * It ensures consistent observability and debugging capabilities throughout
 * the framework.</p>
 *
 * <p>The logger supports JSON-structured logs with consistent fields including
 * timestamp, level, component, message, correlation_id, user_id, request_id,
 * and performance metrics.</p>
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
public class VisionLogger {

    // Performance metrics
    private static final AtomicLong totalRequests = new AtomicLong(0);
    private static final AtomicLong successfulRequests = new AtomicLong(0);
    private static final AtomicLong failedRequests = new AtomicLong(0);
    private static final AtomicLong totalProcessingTime = new AtomicLong(0);

    // Correlation ID tracking
    private static final ThreadLocal<String> correlationIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> requestIdHolder = new ThreadLocal<>();

    // Component-specific loggers
    private static final Map<String, Logger> componentLoggers = new ConcurrentHashMap<>();

    // Log levels
    public enum LogLevel {
        ERROR, WARN, INFO, DEBUG, TRACE
    }

    /**
     * Logs the start of a vision processing operation.
     */
    public static void logOperationStart(String component, String operation, ImageData imageData, DetectionQuery query) {
        String correlationId = getOrCreateCorrelationId();

        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", Instant.now().toString());
        logData.put("level", LogLevel.INFO.name());
        logData.put("component", component);
        logData.put("operation", operation);
        logData.put("correlation_id", correlationId);
        logData.put("user_id", getUserId());
        logData.put("request_id", getRequestId());
        logData.put("image_size", imageData.data().length);
        logData.put("image_format", imageData.format());
        logData.put("detection_type", query.getType().name());
        logData.put("min_confidence", query.getMinConfidence());
        logData.put("max_detections", query.getMaxDetections());
        logData.put("message", "Vision operation started");

        getComponentLogger(component).info("Vision operation started: {}", logData);
        totalRequests.incrementAndGet();
    }

    /**
     * Logs the successful completion of a vision processing operation.
     */
    public static void logOperationSuccess(String component, String operation, List<Detection> results, long processingTimeMs) {
        String correlationId = getOrCreateCorrelationId();

        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", Instant.now().toString());
        logData.put("level", LogLevel.INFO.name());
        logData.put("component", component);
        logData.put("operation", operation);
        logData.put("correlation_id", correlationId);
        logData.put("user_id", getUserId());
        logData.put("request_id", getRequestId());
        logData.put("detection_count", results.size());
        logData.put("processing_time_ms", processingTimeMs);
        logData.put("success", true);
        logData.put("message", "Vision operation completed successfully");

        getComponentLogger(component).info("Vision operation completed successfully: {}", logData);
        successfulRequests.incrementAndGet();
        totalProcessingTime.addAndGet(processingTimeMs);
    }

    /**
     * Logs the failure of a vision processing operation.
     */
    public static void logOperationFailure(String component, String operation, BaseVisionException exception, long processingTimeMs) {
        String correlationId = getOrCreateCorrelationId();

        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", Instant.now().toString());
        logData.put("level", LogLevel.ERROR.name());
        logData.put("component", component);
        logData.put("operation", operation);
        logData.put("correlation_id", correlationId);
        logData.put("user_id", getUserId());
        logData.put("request_id", getRequestId());
        logData.put("error_type", exception.getClass().getSimpleName());
        logData.put("error_message", exception.getMessage());
        logData.put("processing_time_ms", processingTimeMs);
        logData.put("success", false);
        logData.put("message", "Vision operation failed");

        getComponentLogger(component).error("Vision operation failed: {}", logData, exception);
        failedRequests.incrementAndGet();
        totalProcessingTime.addAndGet(processingTimeMs);
    }

    /**
     * Logs a debug message with correlation context.
     */
    public static void debug(String component, String message, Map<String, Object> additionalData) {
        Map<String, Object> logData = createLogData(LogLevel.DEBUG, component, message, additionalData);
        getComponentLogger(component).debug("Debug message: {}", logData);
    }

    /**
     * Logs an info message with correlation context.
     */
    public static void info(String component, String message, Map<String, Object> additionalData) {
        Map<String, Object> logData = createLogData(LogLevel.INFO, component, message, additionalData);
        getComponentLogger(component).info("Info message: {}", logData);
    }

    /**
     * Logs a warning message with correlation context.
     */
    public static void warn(String component, String message, Map<String, Object> additionalData) {
        Map<String, Object> logData = createLogData(LogLevel.WARN, component, message, additionalData);
        getComponentLogger(component).warn("Warning message: {}", logData);
    }

    /**
     * Logs an error message with correlation context.
     */
    public static void error(String component, String message, Throwable exception, Map<String, Object> additionalData) {
        Map<String, Object> logData = createLogData(LogLevel.ERROR, component, message, additionalData);
        getComponentLogger(component).error("Error message: {}", logData, exception);
    }

    /**
     * Logs performance metrics.
     */
    public static void logPerformanceMetrics(String component, String operation, long processingTimeMs, int detectionCount) {
        Map<String, Object> metrics = Map.of(
            "component", component,
            "operation", operation,
            "processing_time_ms", processingTimeMs,
            "detection_count", detectionCount,
            "detections_per_second", detectionCount > 0 ? (detectionCount * 1000.0 / processingTimeMs) : 0.0,
            "message", "Performance metrics"
        );

        debug(component, "Performance metrics recorded", metrics);
    }

    /**
     * Logs security events.
     */
    public static void logSecurityEvent(String component, String eventType, String details, Map<String, Object> context) {
        Map<String, Object> securityData = Map.of(
            "event_type", eventType,
            "details", details,
            "ip_address", context.getOrDefault("ip_address", "unknown"),
            "user_agent", context.getOrDefault("user_agent", "unknown"),
            "message", "Security event detected"
        );

        warn(component, "Security event detected", securityData);
    }

    /**
     * Logs model loading events.
     */
    public static void logModelLoad(String component, String modelName, String modelPath, long loadTimeMs) {
        Map<String, Object> modelData = Map.of(
            "model_name", modelName,
            "model_path", modelPath,
            "load_time_ms", loadTimeMs,
            "message", "Model loaded successfully"
        );

        info(component, "Model loaded successfully", modelData);
    }

    /**
     * Logs model download events.
     */
    public static void logModelDownload(String component, String modelName, String url, long downloadTimeMs, long fileSize) {
        Map<String, Object> downloadData = Map.of(
            "model_name", modelName,
            "download_url", url,
            "download_time_ms", downloadTimeMs,
            "file_size_bytes", fileSize,
            "download_speed_mbps", fileSize > 0 ? (fileSize * 8.0 / (downloadTimeMs * 1000000)) : 0.0,
            "message", "Model downloaded successfully"
        );

        info(component, "Model downloaded successfully", downloadData);
    }

    /**
     * Logs backend health status.
     */
    public static void logHealthStatus(String component, String status, Map<String, Object> healthData) {
        Map<String, Object> healthInfo = Map.of(
            "status", status,
            "health_data", healthData,
            "message", "Health status update"
        );

        info(component, "Health status update", healthInfo);
    }

    /**
     * Sets the correlation ID for the current thread.
     */
    public static void setCorrelationId(String correlationId) {
        correlationIdHolder.set(correlationId);
        MDC.put("correlation_id", correlationId);
    }

    /**
     * Sets the user ID for the current thread.
     */
    public static void setUserId(String userId) {
        userIdHolder.set(userId);
        MDC.put("user_id", userId);
    }

    /**
     * Sets the request ID for the current thread.
     */
    public static void setRequestId(String requestId) {
        requestIdHolder.set(requestId);
        MDC.put("request_id", requestId);
    }

    /**
     * Clears all thread-local context.
     */
    public static void clearContext() {
        correlationIdHolder.remove();
        userIdHolder.remove();
        requestIdHolder.remove();
        MDC.clear();
    }

    /**
     * Gets the current correlation ID or creates a new one.
     */
    public static String getOrCreateCorrelationId() {
        String correlationId = correlationIdHolder.get();
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            setCorrelationId(correlationId);
        }
        return correlationId;
    }

    /**
     * Gets the current user ID.
     */
    public static String getUserId() {
        return userIdHolder.get();
    }

    /**
     * Gets the current request ID.
     */
    public static String getRequestId() {
        return requestIdHolder.get();
    }

    /**
     * Gets performance metrics.
     */
    public static Map<String, Object> getPerformanceMetrics() {
        long total = totalRequests.get();
        long successful = successfulRequests.get();
        long failed = failedRequests.get();
        long totalTime = totalProcessingTime.get();

        return Map.of(
            "total_requests", total,
            "successful_requests", successful,
            "failed_requests", failed,
            "success_rate", total > 0 ? (double) successful / total : 0.0,
            "average_processing_time_ms", total > 0 ? (double) totalTime / total : 0.0,
            "timestamp", Instant.now().toString()
        );
    }

    /**
     * Creates standardized log data with correlation context.
     */
    private static Map<String, Object> createLogData(LogLevel level, String component, String message, Map<String, Object> additionalData) {
        Map<String, Object> baseData = Map.of(
            "timestamp", Instant.now().toString(),
            "level", level.name(),
            "component", component,
            "correlation_id", getOrCreateCorrelationId(),
            "user_id", getUserId(),
            "request_id", getRequestId(),
            "message", message
        );

        if (additionalData != null && !additionalData.isEmpty()) {
            // Merge base data with additional data
            Map<String, Object> mergedData = new java.util.HashMap<>(baseData);
            mergedData.putAll(additionalData);
            return mergedData;
        }

        return baseData;
    }

    /**
     * Gets or creates a component-specific logger.
     */
    private static Logger getComponentLogger(String component) {
        return componentLoggers.computeIfAbsent(component, LoggerFactory::getLogger);
    }

    /**
     * Logs framework startup information.
     */
    public static void logFrameworkStartup(String version, Map<String, Object> startupInfo) {
        Map<String, Object> startupData = Map.of(
            "version", version,
            "startup_info", startupInfo,
            "message", "Spring Vision framework started"
        );

        info("framework", "Spring Vision framework started", startupData);
    }

    /**
     * Logs framework shutdown information.
     */
    public static void logFrameworkShutdown(Map<String, Object> shutdownInfo) {
        Map<String, Object> shutdownData = Map.of(
            "shutdown_info", shutdownInfo,
            "final_metrics", getPerformanceMetrics(),
            "message", "Spring Vision framework shutting down"
        );

        info("framework", "Spring Vision framework shutting down", shutdownData);
    }

    /**
     * Logs configuration changes.
     */
    public static void logConfigurationChange(String component, String configKey, Object oldValue, Object newValue) {
        Map<String, Object> configData = Map.of(
            "config_key", configKey,
            "old_value", oldValue,
            "new_value", newValue,
            "message", "Configuration changed"
        );

        info(component, "Configuration changed", configData);
    }

    /**
     * Logs resource usage metrics.
     */
    public static void logResourceUsage(String component, Map<String, Object> resourceMetrics) {
        Map<String, Object> resourceData = Map.of(
            "resource_metrics", resourceMetrics,
            "message", "Resource usage metrics"
        );

        debug(component, "Resource usage metrics", resourceData);
    }

    /**
     * Logs cache operations.
     */
    public static void logCacheOperation(String component, String operation, String key, boolean success, long operationTimeMs) {
        Map<String, Object> cacheData = Map.of(
            "operation", operation,
            "cache_key", key,
            "success", success,
            "operation_time_ms", operationTimeMs,
            "message", "Cache operation performed"
        );

        debug(component, "Cache operation performed", cacheData);
    }

    /**
     * Logs database operations.
     */
    public static void logDatabaseOperation(String component, String operation, String table, boolean success, long operationTimeMs) {
        Map<String, Object> dbData = Map.of(
            "operation", operation,
            "table", table,
            "success", success,
            "operation_time_ms", operationTimeMs,
            "message", "Database operation performed"
        );

        debug(component, "Database operation performed", dbData);
    }

    /**
     * Logs external API calls.
     */
    public static void logExternalApiCall(String component, String endpoint, String method, int statusCode, long responseTimeMs) {
        Map<String, Object> apiData = Map.of(
            "endpoint", endpoint,
            "method", method,
            "status_code", statusCode,
            "response_time_ms", responseTimeMs,
            "success", statusCode >= 200 && statusCode < 300,
            "message", "External API call completed"
        );

        info(component, "External API call completed", apiData);
    }

    /**
     * Logs batch processing events.
     */
    public static void logBatchProcessing(String component, int totalItems, int processedItems, int failedItems, long totalTimeMs) {
        Map<String, Object> batchData = Map.of(
            "total_items", totalItems,
            "processed_items", processedItems,
            "failed_items", failedItems,
            "success_rate", totalItems > 0 ? (double) processedItems / totalItems : 0.0,
            "total_time_ms", totalTimeMs,
            "items_per_second", totalTimeMs > 0 ? (processedItems * 1000.0 / totalTimeMs) : 0.0,
            "message", "Batch processing completed"
        );

        info(component, "Batch processing completed", batchData);
    }
}
