package com.springvision.core.metrics;

import com.springvision.core.DetectionType;
import com.springvision.core.logging.VisionLogger;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive performance monitoring and metrics collection for the Spring Vision framework.
 * 
 * <p>This class provides detailed metrics collection including performance counters,
 * timing measurements, resource utilization, and business KPIs. It integrates with
 * Micrometer for monitoring system integration.</p>
 * 
 * <p>The metrics system supports custom metrics, performance profiling, bottleneck
 * detection, and resource utilization monitoring.</p>
 * 
 * @since 1.1.0
 * @author Spring Vision Team
 */
@Component
public class VisionMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // Performance counters
    private final Map<String, Counter> requestCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> processingTimers = new ConcurrentHashMap<>();
    private final Map<String, Gauge> resourceGauges = new ConcurrentHashMap<>();
    
    // Business KPIs
    private final AtomicLong totalDetections = new AtomicLong(0);
    private final AtomicLong totalFacesDetected = new AtomicLong(0);
    private final AtomicLong totalObjectsDetected = new AtomicLong(0);
    private final AtomicLong totalHandsDetected = new AtomicLong(0);
    private final AtomicLong totalPosesDetected = new AtomicLong(0);
    
    // Performance metrics
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicLong totalImageSize = new AtomicLong(0);
    private final AtomicLong totalModelLoads = new AtomicLong(0);
    private final AtomicLong totalModelDownloads = new AtomicLong(0);
    
    // Resource utilization
    private final AtomicLong memoryUsage = new AtomicLong(0);
    private final AtomicLong cpuUsage = new AtomicLong(0);
    private final AtomicLong activeThreads = new AtomicLong(0);
    
    public VisionMetrics() {
        this.meterRegistry = new SimpleMeterRegistry();
        initializeMetrics();
    }
    
    public VisionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeMetrics();
    }
    
    /**
     * Initializes all metrics.
     */
    private void initializeMetrics() {
        // Initialize business KPIs
        Gauge.builder("vision.detections.total", totalDetections, AtomicLong::get)
            .description("Total number of detections performed")
            .register(meterRegistry);
            
        Gauge.builder("vision.detections.faces", totalFacesDetected, AtomicLong::get)
            .description("Total number of faces detected")
            .register(meterRegistry);
            
        Gauge.builder("vision.detections.objects", totalObjectsDetected, AtomicLong::get)
            .description("Total number of objects detected")
            .register(meterRegistry);
            
        Gauge.builder("vision.detections.hands", totalHandsDetected, AtomicLong::get)
            .description("Total number of hands detected")
            .register(meterRegistry);
            
        Gauge.builder("vision.detections.poses", totalPosesDetected, AtomicLong::get)
            .description("Total number of poses detected")
            .register(meterRegistry);
        
        // Initialize performance metrics
        Gauge.builder("vision.performance.total_processing_time", totalProcessingTime, AtomicLong::get)
            .description("Total processing time in milliseconds")
            .register(meterRegistry);
            
        Gauge.builder("vision.performance.total_image_size", totalImageSize, AtomicLong::get)
            .description("Total image size processed in bytes")
            .register(meterRegistry);
            
        Gauge.builder("vision.performance.total_model_loads", totalModelLoads, AtomicLong::get)
            .description("Total number of model loads")
            .register(meterRegistry);
            
        Gauge.builder("vision.performance.total_model_downloads", totalModelDownloads, AtomicLong::get)
            .description("Total number of model downloads")
            .register(meterRegistry);
        
        // Initialize resource utilization metrics
        Gauge.builder("vision.resources.memory_usage", memoryUsage, AtomicLong::get)
            .description("Memory usage in bytes")
            .register(meterRegistry);
            
        Gauge.builder("vision.resources.cpu_usage", cpuUsage, AtomicLong::get)
            .description("CPU usage percentage")
            .register(meterRegistry);
            
        Gauge.builder("vision.resources.active_threads", activeThreads, AtomicLong::get)
            .description("Number of active threads")
            .register(meterRegistry);
    }
    
    /**
     * Records a detection request.
     */
    public void recordDetectionRequest(String backend, DetectionType detectionType) {
        String key = backend + "." + detectionType.name().toLowerCase();
        
        Counter counter = requestCounters.computeIfAbsent(key, k -> 
            Counter.builder("vision.requests.total")
                .tag("backend", backend)
                .tag("detection_type", detectionType.name())
                .description("Total detection requests")
                .register(meterRegistry)
        );
        
        counter.increment();
        totalDetections.incrementAndGet();
        
        // Update specific detection counters
        switch (detectionType) {
            case FACE -> totalFacesDetected.incrementAndGet();
            case OBJECT -> totalObjectsDetected.incrementAndGet();
            case HAND -> totalHandsDetected.incrementAndGet();
            case POSE -> totalPosesDetected.incrementAndGet();
        }
    }
    
    /**
     * Records a detection error.
     */
    public void recordDetectionError(String backend, DetectionType detectionType, String errorType) {
        String key = backend + "." + detectionType.name().toLowerCase() + "." + errorType;
        
        Counter counter = errorCounters.computeIfAbsent(key, k -> 
            Counter.builder("vision.errors.total")
                .tag("backend", backend)
                .tag("detection_type", detectionType.name())
                .tag("error_type", errorType)
                .description("Total detection errors")
                .register(meterRegistry)
        );
        
        counter.increment();
    }
    
    /**
     * Records processing time.
     */
    public Timer.Sample startProcessingTimer(String backend, DetectionType detectionType) {
        String key = backend + "." + detectionType.name().toLowerCase();
        
        Timer timer = processingTimers.computeIfAbsent(key, k -> 
            Timer.builder("vision.processing.time")
                .tag("backend", backend)
                .tag("detection_type", detectionType.name())
                .description("Processing time")
                .register(meterRegistry)
        );
        
        return Timer.start(meterRegistry);
    }
    
    /**
     * Stops processing timer and records the time.
     */
    public void stopProcessingTimer(Timer.Sample sample, String backend, DetectionType detectionType) {
        String key = backend + "." + detectionType.name().toLowerCase();
        Timer timer = processingTimers.get(key);
        
        if (timer != null) {
            long duration = sample.stop(timer);
            totalProcessingTime.addAndGet(duration);
            
            // Log performance metrics
            Map<String, Object> metrics = Map.of(
                "backend", backend,
                "detection_type", detectionType.name(),
                "processing_time_ms", duration,
                "message", "Performance metrics recorded"
            );
            
            VisionLogger.logPerformanceMetrics(backend, "detection", duration, 0);
        }
    }
    
    /**
     * Records image size processed.
     */
    public void recordImageSize(String backend, long imageSizeBytes) {
        totalImageSize.addAndGet(imageSizeBytes);
        
        // Record as histogram
        Histogram.builder("vision.image.size")
            .tag("backend", backend)
            .description("Image size distribution")
            .register(meterRegistry)
            .record(imageSizeBytes);
    }
    
    /**
     * Records model load.
     */
    public void recordModelLoad(String backend, String modelName, long loadTimeMs) {
        totalModelLoads.incrementAndGet();
        
        Timer.builder("vision.model.load.time")
            .tag("backend", backend)
            .tag("model", modelName)
            .description("Model load time")
            .register(meterRegistry)
            .record(loadTimeMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Records model download.
     */
    public void recordModelDownload(String backend, String modelName, long downloadTimeMs, long fileSizeBytes) {
        totalModelDownloads.incrementAndGet();
        
        Timer.builder("vision.model.download.time")
            .tag("backend", backend)
            .tag("model", modelName)
            .description("Model download time")
            .register(meterRegistry)
            .record(downloadTimeMs, TimeUnit.MILLISECONDS);
            
        Histogram.builder("vision.model.download.size")
            .tag("backend", backend)
            .tag("model", modelName)
            .description("Model download size")
            .register(meterRegistry)
            .record(fileSizeBytes);
    }
    
    /**
     * Records resource utilization.
     */
    public void recordResourceUtilization(long memoryUsageBytes, long cpuUsagePercent, long activeThreadCount) {
        memoryUsage.set(memoryUsageBytes);
        cpuUsage.set(cpuUsagePercent);
        activeThreads.set(activeThreadCount);
        
        // Record as gauges
        Gauge.builder("vision.resources.memory_usage_bytes")
            .description("Memory usage in bytes")
            .register(meterRegistry, memoryUsage, AtomicLong::get);
            
        Gauge.builder("vision.resources.cpu_usage_percent")
            .description("CPU usage percentage")
            .register(meterRegistry, cpuUsage, AtomicLong::get);
            
        Gauge.builder("vision.resources.active_thread_count")
            .description("Active thread count")
            .register(meterRegistry, activeThreads, AtomicLong::get);
    }
    
    /**
     * Records cache hit/miss.
     */
    public void recordCacheHit(String backend, String cacheType) {
        Counter.builder("vision.cache.hits")
            .tag("backend", backend)
            .tag("cache_type", cacheType)
            .description("Cache hits")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Records cache miss.
     */
    public void recordCacheMiss(String backend, String cacheType) {
        Counter.builder("vision.cache.misses")
            .tag("backend", backend)
            .tag("cache_type", cacheType)
            .description("Cache misses")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Records database operation.
     */
    public void recordDatabaseOperation(String backend, String operation, long durationMs, boolean success) {
        Timer.builder("vision.database.operation.time")
            .tag("backend", backend)
            .tag("operation", operation)
            .tag("success", String.valueOf(success))
            .description("Database operation time")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Records external API call.
     */
    public void recordExternalApiCall(String backend, String endpoint, long durationMs, int statusCode) {
        Timer.builder("vision.external.api.time")
            .tag("backend", backend)
            .tag("endpoint", endpoint)
            .tag("status_code", String.valueOf(statusCode))
            .description("External API call time")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Records batch processing metrics.
     */
    public void recordBatchProcessing(String backend, int totalItems, int processedItems, int failedItems, long totalTimeMs) {
        Histogram.builder("vision.batch.total_items")
            .tag("backend", backend)
            .description("Batch total items")
            .register(meterRegistry)
            .record(totalItems);
            
        Histogram.builder("vision.batch.processed_items")
            .tag("backend", backend)
            .description("Batch processed items")
            .register(meterRegistry)
            .record(processedItems);
            
        Histogram.builder("vision.batch.failed_items")
            .tag("backend", backend)
            .description("Batch failed items")
            .register(meterRegistry)
            .record(failedItems);
            
        Timer.builder("vision.batch.processing.time")
            .tag("backend", backend)
            .description("Batch processing time")
            .register(meterRegistry)
            .record(totalTimeMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Records custom metric.
     */
    public void recordCustomMetric(String name, double value, String... tags) {
        Gauge.builder("vision.custom." + name)
            .description("Custom metric: " + name)
            .register(meterRegistry, value, v -> v);
    }
    
    /**
     * Records custom counter.
     */
    public void incrementCustomCounter(String name, String... tags) {
        Counter.builder("vision.custom." + name)
            .description("Custom counter: " + name)
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * Records custom timer.
     */
    public Timer.Sample startCustomTimer(String name, String... tags) {
        Timer timer = Timer.builder("vision.custom." + name)
            .description("Custom timer: " + name)
            .register(meterRegistry);
            
        return Timer.start(meterRegistry);
    }
    
    /**
     * Gets comprehensive metrics summary.
     */
    public MetricsSummary getMetricsSummary() {
        return MetricsSummary.builder()
            .totalDetections(totalDetections.get())
            .totalFacesDetected(totalFacesDetected.get())
            .totalObjectsDetected(totalObjectsDetected.get())
            .totalHandsDetected(totalHandsDetected.get())
            .totalPosesDetected(totalPosesDetected.get())
            .totalProcessingTime(totalProcessingTime.get())
            .totalImageSize(totalImageSize.get())
            .totalModelLoads(totalModelLoads.get())
            .totalModelDownloads(totalModelDownloads.get())
            .memoryUsage(memoryUsage.get())
            .cpuUsage(cpuUsage.get())
            .activeThreads(activeThreads.get())
            .build();
    }
    
    /**
     * Gets the meter registry.
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
    
    /**
     * Metrics summary data class.
     */
    public static class MetricsSummary {
        private final long totalDetections;
        private final long totalFacesDetected;
        private final long totalObjectsDetected;
        private final long totalHandsDetected;
        private final long totalPosesDetected;
        private final long totalProcessingTime;
        private final long totalImageSize;
        private final long totalModelLoads;
        private final long totalModelDownloads;
        private final long memoryUsage;
        private final long cpuUsage;
        private final long activeThreads;
        
        private MetricsSummary(Builder builder) {
            this.totalDetections = builder.totalDetections;
            this.totalFacesDetected = builder.totalFacesDetected;
            this.totalObjectsDetected = builder.totalObjectsDetected;
            this.totalHandsDetected = builder.totalHandsDetected;
            this.totalPosesDetected = builder.totalPosesDetected;
            this.totalProcessingTime = builder.totalProcessingTime;
            this.totalImageSize = builder.totalImageSize;
            this.totalModelLoads = builder.totalModelLoads;
            this.totalModelDownloads = builder.totalModelDownloads;
            this.memoryUsage = builder.memoryUsage;
            this.cpuUsage = builder.cpuUsage;
            this.activeThreads = builder.activeThreads;
        }
        
        // Getters
        public long getTotalDetections() { return totalDetections; }
        public long getTotalFacesDetected() { return totalFacesDetected; }
        public long getTotalObjectsDetected() { return totalObjectsDetected; }
        public long getTotalHandsDetected() { return totalHandsDetected; }
        public long getTotalPosesDetected() { return totalPosesDetected; }
        public long getTotalProcessingTime() { return totalProcessingTime; }
        public long getTotalImageSize() { return totalImageSize; }
        public long getTotalModelLoads() { return totalModelLoads; }
        public long getTotalModelDownloads() { return totalModelDownloads; }
        public long getMemoryUsage() { return memoryUsage; }
        public long getCpuUsage() { return cpuUsage; }
        public long getActiveThreads() { return activeThreads; }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private long totalDetections;
            private long totalFacesDetected;
            private long totalObjectsDetected;
            private long totalHandsDetected;
            private long totalPosesDetected;
            private long totalProcessingTime;
            private long totalImageSize;
            private long totalModelLoads;
            private long totalModelDownloads;
            private long memoryUsage;
            private long cpuUsage;
            private long activeThreads;
            
            public Builder totalDetections(long totalDetections) {
                this.totalDetections = totalDetections;
                return this;
            }
            
            public Builder totalFacesDetected(long totalFacesDetected) {
                this.totalFacesDetected = totalFacesDetected;
                return this;
            }
            
            public Builder totalObjectsDetected(long totalObjectsDetected) {
                this.totalObjectsDetected = totalObjectsDetected;
                return this;
            }
            
            public Builder totalHandsDetected(long totalHandsDetected) {
                this.totalHandsDetected = totalHandsDetected;
                return this;
            }
            
            public Builder totalPosesDetected(long totalPosesDetected) {
                this.totalPosesDetected = totalPosesDetected;
                return this;
            }
            
            public Builder totalProcessingTime(long totalProcessingTime) {
                this.totalProcessingTime = totalProcessingTime;
                return this;
            }
            
            public Builder totalImageSize(long totalImageSize) {
                this.totalImageSize = totalImageSize;
                return this;
            }
            
            public Builder totalModelLoads(long totalModelLoads) {
                this.totalModelLoads = totalModelLoads;
                return this;
            }
            
            public Builder totalModelDownloads(long totalModelDownloads) {
                this.totalModelDownloads = totalModelDownloads;
                return this;
            }
            
            public Builder memoryUsage(long memoryUsage) {
                this.memoryUsage = memoryUsage;
                return this;
            }
            
            public Builder cpuUsage(long cpuUsage) {
                this.cpuUsage = cpuUsage;
                return this;
            }
            
            public Builder activeThreads(long activeThreads) {
                this.activeThreads = activeThreads;
                return this;
            }
            
            public MetricsSummary build() {
                return new MetricsSummary(this);
            }
        }
    }
} 