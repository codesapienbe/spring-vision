package com.springvision.core.benchmark;

import com.springvision.core.*;
import com.springvision.core.metrics.VisionMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Comprehensive performance benchmarking system for the Spring Vision framework.
 * 
 * <p>This class provides detailed performance analysis including throughput,
 * latency, resource utilization, and comparative analysis between different
 * backends and configurations.</p>
 * 
 * <p>The benchmarking system supports load testing, stress testing, and
 * performance profiling for production readiness assessment.</p>
 * 
 * @since 1.1.0
 * @author Spring Vision Team
 */
@Component
public class PerformanceBenchmark {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceBenchmark.class);
    
    private final VisionTemplate visionTemplate;
    private final VisionMetrics visionMetrics;
    
    // Benchmark configuration
    private static final int DEFAULT_WARMUP_ITERATIONS = 10;
    private static final int DEFAULT_BENCHMARK_ITERATIONS = 100;
    private static final int DEFAULT_CONCURRENT_THREADS = 4;
    private static final long DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    
    public PerformanceBenchmark(VisionTemplate visionTemplate, VisionMetrics visionMetrics) {
        this.visionTemplate = visionTemplate;
        this.visionMetrics = visionMetrics;
    }
    
    /**
     * Runs comprehensive performance benchmark for all backends.
     */
    public BenchmarkReport runFullBenchmark(byte[] testImageData) {
        logger.info("Starting comprehensive performance benchmark");
        
        // Create test image data
        ImageData testImage = new ImageData(testImageData, "image/jpeg", testImageData.length, "JPEG");
        BenchmarkReport report = new BenchmarkReport();
        
        // Test face detection
        DetectionQuery faceQuery = new DetectionQuery.Builder()
            .type(DetectionType.FACE)
            .minConfidence(0.5)
            .maxDetections(10)
            .build();
        
        logger.info("Testing face detection performance...");
        long startTime = System.currentTimeMillis();
        VisionResult faceResult = visionTemplate.detect(testImage, faceQuery);
        List<Detection> faceResults = faceResult.detections();
        long faceDetectionTime = System.currentTimeMillis() - startTime;
        
        report.addMetric("face_detection_time_ms", faceDetectionTime);
        report.addMetric("face_detection_count", faceResults.size());
        
        // Test object detection
        DetectionQuery objectQuery = new DetectionQuery.Builder()
            .type(DetectionType.OBJECT)
            .minConfidence(0.5)
            .maxDetections(20)
            .build();
        
        logger.info("Testing object detection performance...");
        startTime = System.currentTimeMillis();
        VisionResult objectResult = visionTemplate.detect(testImage, objectQuery);
        List<Detection> objectResults = objectResult.detections();
        long objectDetectionTime = System.currentTimeMillis() - startTime;
        
        report.addMetric("object_detection_time_ms", objectDetectionTime);
        report.addMetric("object_detection_count", objectResults.size());
        
        // Test batch processing
        logger.info("Testing batch processing performance...");
        List<ImageData> batchImages = Arrays.asList(testImage, testImage, testImage);
        DetectionQuery batchQuery = new DetectionQuery.Builder()
            .type(DetectionType.FACE)
            .minConfidence(0.5)
            .maxDetections(5)
            .build();
        
        startTime = System.currentTimeMillis();
        List<List<Detection>> batchResults = new ArrayList<>();
        for (ImageData image : batchImages) {
            VisionResult batchResult = visionTemplate.detect(image, batchQuery);
            batchResults.add(batchResult.detections());
        }
        long batchProcessingTime = System.currentTimeMillis() - startTime;
        
        report.addMetric("batch_processing_time_ms", batchProcessingTime);
        report.addMetric("batch_image_count", batchImages.size());
        
        // Test memory usage
        logger.info("Testing memory usage...");
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Perform multiple detections to measure memory impact
        for (int i = 0; i < 10; i++) {
            visionTemplate.detect(testImage, faceQuery);
        }
        
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        report.addMetric("memory_usage_bytes", memoryUsed);
        
        // Test concurrent processing
        logger.info("Testing concurrent processing performance...");
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        DetectionQuery concurrentQuery = new DetectionQuery.Builder()
            .type(DetectionType.FACE)
            .minConfidence(0.5)
            .maxDetections(5)
            .build();
        
        startTime = System.currentTimeMillis();
        List<CompletableFuture<VisionResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> 
                visionTemplate.detect(testImage, concurrentQuery), executor));
        }
        
        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long concurrentProcessingTime = System.currentTimeMillis() - startTime;
        
        executor.shutdown();
        
        report.addMetric("concurrent_processing_time_ms", concurrentProcessingTime);
        report.addMetric("concurrent_thread_count", threadCount);
        
        // Generate comparative analysis
        report.generateComparativeAnalysis();
        
        logger.info("Performance benchmark completed: {}", report.getSummary());
        return report;
    }
    
    /**
     * Benchmarks a specific detection type.
     */
    public BenchmarkResult benchmarkDetectionType(ImageData testImage, DetectionType detectionType) {
        logger.info("Benchmarking detection type: {}", detectionType);
        
        DetectionQuery query = new DetectionQuery.Builder()
            .type(detectionType)
            .minConfidence(0.5)
            .maxDetections(10)
            .build();
        
        // Warmup phase
        warmup(testImage, query);
        
        // Benchmark phase
        List<Long> processingTimes = new ArrayList<>();
        List<Integer> detectionCounts = new ArrayList<>();
        
        for (int i = 0; i < DEFAULT_BENCHMARK_ITERATIONS; i++) {
            long startTime = System.currentTimeMillis();
            
            VisionResult result = visionTemplate.detect(testImage, query);
            List<Detection> detections = result.detections();
            
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            processingTimes.add(processingTime);
            detectionCounts.add(detections.size());
            
            // Log progress
            if ((i + 1) % 20 == 0) {
                logger.debug("Benchmark progress: {}/{} iterations completed", i + 1, DEFAULT_BENCHMARK_ITERATIONS);
            }
        }
        
        return createBenchmarkResult(detectionType, processingTimes, detectionCounts);
    }
    
    /**
     * Runs concurrent load testing.
     */
    public LoadTestResult runLoadTest(ImageData testImage, DetectionType detectionType, 
                                    int concurrentUsers, int requestsPerUser) {
        logger.info("Starting load test: {} users, {} requests per user", concurrentUsers, requestsPerUser);
        
        DetectionQuery query = new DetectionQuery.Builder()
            .type(detectionType)
            .minConfidence(0.5)
            .maxDetections(10)
            .build();
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        List<Future<LoadTestUserResult>> futures = new ArrayList<>();
        
        // Submit tasks for each user
        for (int user = 0; user < concurrentUsers; user++) {
            final int userId = user;
            futures.add(executor.submit(() -> simulateUser(testImage, query, requestsPerUser, userId)));
        }
        
        // Collect results
        List<LoadTestUserResult> userResults = new ArrayList<>();
        for (Future<LoadTestUserResult> future : futures) {
            try {
                userResults.add(future.get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            } catch (Exception e) {
                logger.error("Load test user failed", e);
            }
        }
        
        executor.shutdown();
        
        return createLoadTestResult(userResults);
    }
    
    /**
     * Runs stress testing to find system limits.
     */
    public StressTestResult runStressTest(ImageData testImage, DetectionType detectionType) {
        logger.info("Starting stress test for detection type: {}", detectionType);
        
        DetectionQuery query = new DetectionQuery.Builder()
            .type(detectionType)
            .minConfidence(0.5)
            .maxDetections(10)
            .build();
        
        List<StressTestPhase> phases = new ArrayList<>();
        
        // Gradually increase load
        for (int concurrentUsers = 1; concurrentUsers <= 32; concurrentUsers *= 2) {
            StressTestPhase phase = runStressTestPhase(testImage, query, concurrentUsers);
            phases.add(phase);
            
            // Stop if error rate is too high
            if (phase.getErrorRate() > 0.1) { // 10% error rate threshold
                logger.warn("Stress test stopped due to high error rate: {}%", phase.getErrorRate() * 100);
                break;
            }
        }
        
        return new StressTestResult(detectionType, phases);
    }
    
    /**
     * Runs memory usage analysis.
     */
    public MemoryAnalysisResult runMemoryAnalysis(ImageData testImage, DetectionType detectionType, int iterations) {
        logger.info("Starting memory analysis: {} iterations", iterations);
        
        DetectionQuery query = new DetectionQuery.Builder()
            .type(detectionType)
            .minConfidence(0.5)
            .maxDetections(10)
            .build();
        
        List<Long> memoryUsage = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection before starting
        System.gc();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        for (int i = 0; i < iterations; i++) {
            long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
            
            visionTemplate.detect(testImage, query);
            
            long afterMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = afterMemory - beforeMemory;
            
            memoryUsage.add(memoryUsed);
            
            // Force garbage collection every 10 iterations
            if ((i + 1) % 10 == 0) {
                System.gc();
            }
        }
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long totalMemoryIncrease = finalMemory - initialMemory;
        
        return new MemoryAnalysisResult(detectionType, memoryUsage, totalMemoryIncrease);
    }
    
    /**
     * Runs resource utilization analysis.
     */
    public ResourceUtilizationResult runResourceAnalysis(ImageData testImage, DetectionType detectionType, 
                                                       int durationSeconds) {
        logger.info("Starting resource analysis: {} seconds", durationSeconds);
        
        DetectionQuery query = new DetectionQuery.Builder()
            .type(detectionType)
            .minConfidence(0.5)
            .maxDetections(10)
            .build();
        
        List<ResourceSnapshot> snapshots = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds * 1000L);
        
        while (System.currentTimeMillis() < endTime) {
            // Take resource snapshot
            ResourceSnapshot snapshot = takeResourceSnapshot();
            snapshots.add(snapshot);
            
            // Perform detection
            visionTemplate.detect(testImage, query);
            
            // Wait 100ms between snapshots
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return new ResourceUtilizationResult(detectionType, snapshots);
    }
    
    /**
     * Warmup phase to stabilize performance.
     */
    private void warmup(ImageData testImage, DetectionQuery query) {
        logger.debug("Running warmup phase: {} iterations", DEFAULT_WARMUP_ITERATIONS);
        
        for (int i = 0; i < DEFAULT_WARMUP_ITERATIONS; i++) {
            try {
                visionTemplate.detect(testImage, query);
            } catch (Exception e) {
                logger.warn("Warmup iteration failed: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Simulates a single user in load testing.
     */
    private LoadTestUserResult simulateUser(ImageData testImage, DetectionQuery query, 
                                          int requests, int userId) {
        List<Long> responseTimes = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        
        for (int i = 0; i < requests; i++) {
            long startTime = System.currentTimeMillis();
            
            try {
                visionTemplate.detect(testImage, query);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                logger.debug("User {} request {} failed: {}", userId, i, e.getMessage());
            }
            
            long endTime = System.currentTimeMillis();
            responseTimes.add(endTime - startTime);
            
            // Add small delay between requests
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return new LoadTestUserResult(userId, responseTimes, successCount, errorCount);
    }
    
    /**
     * Runs a single stress test phase.
     */
    private StressTestPhase runStressTestPhase(ImageData testImage, DetectionQuery query, int concurrentUsers) {
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        List<Future<Boolean>> futures = new ArrayList<>();
        
        // Submit tasks
        for (int i = 0; i < concurrentUsers; i++) {
            futures.add(executor.submit(() -> {
                try {
                    visionTemplate.detect(testImage, query);
                    return true; // Success
                } catch (Exception e) {
                    return false; // Failure
                }
            }));
        }
        
        // Collect results
        int successCount = 0;
        int totalCount = futures.size();
        
        for (Future<Boolean> future : futures) {
            try {
                if (future.get(5, TimeUnit.SECONDS)) {
                    successCount++;
                }
            } catch (Exception e) {
                // Count as failure
            }
        }
        
        executor.shutdown();
        
        double successRate = (double) successCount / totalCount;
        double errorRate = 1.0 - successRate;
        
        return new StressTestPhase(concurrentUsers, successRate, errorRate);
    }
    
    /**
     * Takes a resource utilization snapshot.
     */
    private ResourceSnapshot takeResourceSnapshot() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        // Get CPU usage (simplified)
        long cpuTime = System.nanoTime();
        
        // Get thread count
        int threadCount = Thread.activeCount();
        
        return new ResourceSnapshot(
            System.currentTimeMillis(),
            usedMemory,
            totalMemory,
            maxMemory,
            cpuTime,
            threadCount
        );
    }
    
    /**
     * Creates benchmark result from collected data.
     */
    private BenchmarkResult createBenchmarkResult(DetectionType detectionType, 
                                                List<Long> processingTimes, 
                                                List<Integer> detectionCounts) {
        // Calculate statistics
        double avgProcessingTime = processingTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
            
        double avgDetectionCount = detectionCounts.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
            
        long minProcessingTime = processingTimes.stream()
            .mapToLong(Long::longValue)
            .min()
            .orElse(0L);
            
        long maxProcessingTime = processingTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0L);
            
        // Calculate percentiles
        List<Long> sortedTimes = processingTimes.stream().sorted().collect(Collectors.toList());
        long p50 = calculatePercentile(sortedTimes, 50);
        long p95 = calculatePercentile(sortedTimes, 95);
        long p99 = calculatePercentile(sortedTimes, 99);
        
        // Calculate throughput
        double throughput = 1000.0 / avgProcessingTime; // requests per second
        
        return new BenchmarkResult(
            detectionType,
            avgProcessingTime,
            minProcessingTime,
            maxProcessingTime,
            p50,
            p95,
            p99,
            throughput,
            avgDetectionCount,
            processingTimes.size()
        );
    }
    
    /**
     * Creates load test result from user results.
     */
    private LoadTestResult createLoadTestResult(List<LoadTestUserResult> userResults) {
        int totalUsers = userResults.size();
        int totalRequests = userResults.stream().mapToInt(r -> r.getSuccessCount() + r.getErrorCount()).sum();
        int totalSuccess = userResults.stream().mapToInt(LoadTestUserResult::getSuccessCount).sum();
        int totalErrors = userResults.stream().mapToInt(LoadTestUserResult::getErrorCount).sum();
        
        List<Long> allResponseTimes = userResults.stream()
            .flatMap(r -> r.getResponseTimes().stream())
            .collect(Collectors.toList());
            
        double avgResponseTime = allResponseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
            
        double successRate = (double) totalSuccess / totalRequests;
        double errorRate = (double) totalErrors / totalRequests;
        
        return new LoadTestResult(
            totalUsers,
            totalRequests,
            totalSuccess,
            totalErrors,
            avgResponseTime,
            successRate,
            errorRate,
            allResponseTimes
        );
    }
    
    /**
     * Calculates percentile from sorted list.
     */
    private long calculatePercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0L;
        }
        
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, index));
    }
    
    /**
     * Benchmark result data class.
     */
    public static class BenchmarkResult {
        private final DetectionType detectionType;
        private final double avgProcessingTime;
        private final long minProcessingTime;
        private final long maxProcessingTime;
        private final long p50ProcessingTime;
        private final long p95ProcessingTime;
        private final long p99ProcessingTime;
        private final double throughput;
        private final double avgDetectionCount;
        private final int iterations;
        
        public BenchmarkResult(DetectionType detectionType, double avgProcessingTime, 
                             long minProcessingTime, long maxProcessingTime,
                             long p50ProcessingTime, long p95ProcessingTime, long p99ProcessingTime,
                             double throughput, double avgDetectionCount, int iterations) {
            this.detectionType = detectionType;
            this.avgProcessingTime = avgProcessingTime;
            this.minProcessingTime = minProcessingTime;
            this.maxProcessingTime = maxProcessingTime;
            this.p50ProcessingTime = p50ProcessingTime;
            this.p95ProcessingTime = p95ProcessingTime;
            this.p99ProcessingTime = p99ProcessingTime;
            this.throughput = throughput;
            this.avgDetectionCount = avgDetectionCount;
            this.iterations = iterations;
        }
        
        // Getters
        public DetectionType getDetectionType() { return detectionType; }
        public double getAvgProcessingTime() { return avgProcessingTime; }
        public long getMinProcessingTime() { return minProcessingTime; }
        public long getMaxProcessingTime() { return maxProcessingTime; }
        public long getP50ProcessingTime() { return p50ProcessingTime; }
        public long getP95ProcessingTime() { return p95ProcessingTime; }
        public long getP99ProcessingTime() { return p99ProcessingTime; }
        public double getThroughput() { return throughput; }
        public double getAvgDetectionCount() { return avgDetectionCount; }
        public int getIterations() { return iterations; }
    }
    
    /**
     * Load test user result data class.
     */
    public static class LoadTestUserResult {
        private final int userId;
        private final List<Long> responseTimes;
        private final int successCount;
        private final int errorCount;
        
        public LoadTestUserResult(int userId, List<Long> responseTimes, int successCount, int errorCount) {
            this.userId = userId;
            this.responseTimes = responseTimes;
            this.successCount = successCount;
            this.errorCount = errorCount;
        }
        
        // Getters
        public int getUserId() { return userId; }
        public List<Long> getResponseTimes() { return responseTimes; }
        public int getSuccessCount() { return successCount; }
        public int getErrorCount() { return errorCount; }
    }
    
    /**
     * Load test result data class.
     */
    public static class LoadTestResult {
        private final int totalUsers;
        private final int totalRequests;
        private final int totalSuccess;
        private final int totalErrors;
        private final double avgResponseTime;
        private final double successRate;
        private final double errorRate;
        private final List<Long> allResponseTimes;
        
        public LoadTestResult(int totalUsers, int totalRequests, int totalSuccess, int totalErrors,
                            double avgResponseTime, double successRate, double errorRate,
                            List<Long> allResponseTimes) {
            this.totalUsers = totalUsers;
            this.totalRequests = totalRequests;
            this.totalSuccess = totalSuccess;
            this.totalErrors = totalErrors;
            this.avgResponseTime = avgResponseTime;
            this.successRate = successRate;
            this.errorRate = errorRate;
            this.allResponseTimes = allResponseTimes;
        }
        
        // Getters
        public int getTotalUsers() { return totalUsers; }
        public int getTotalRequests() { return totalRequests; }
        public int getTotalSuccess() { return totalSuccess; }
        public int getTotalErrors() { return totalErrors; }
        public double getAvgResponseTime() { return avgResponseTime; }
        public double getSuccessRate() { return successRate; }
        public double getErrorRate() { return errorRate; }
        public List<Long> getAllResponseTimes() { return allResponseTimes; }
    }
    
    /**
     * Stress test phase data class.
     */
    public static class StressTestPhase {
        private final int concurrentUsers;
        private final double successRate;
        private final double errorRate;
        
        public StressTestPhase(int concurrentUsers, double successRate, double errorRate) {
            this.concurrentUsers = concurrentUsers;
            this.successRate = successRate;
            this.errorRate = errorRate;
        }
        
        // Getters
        public int getConcurrentUsers() { return concurrentUsers; }
        public double getSuccessRate() { return successRate; }
        public double getErrorRate() { return errorRate; }
    }
    
    /**
     * Stress test result data class.
     */
    public static class StressTestResult {
        private final DetectionType detectionType;
        private final List<StressTestPhase> phases;
        
        public StressTestResult(DetectionType detectionType, List<StressTestPhase> phases) {
            this.detectionType = detectionType;
            this.phases = phases;
        }
        
        // Getters
        public DetectionType getDetectionType() { return detectionType; }
        public List<StressTestPhase> getPhases() { return phases; }
    }
    
    /**
     * Memory analysis result data class.
     */
    public static class MemoryAnalysisResult {
        private final DetectionType detectionType;
        private final List<Long> memoryUsage;
        private final long totalMemoryIncrease;
        
        public MemoryAnalysisResult(DetectionType detectionType, List<Long> memoryUsage, long totalMemoryIncrease) {
            this.detectionType = detectionType;
            this.memoryUsage = memoryUsage;
            this.totalMemoryIncrease = totalMemoryIncrease;
        }
        
        // Getters
        public DetectionType getDetectionType() { return detectionType; }
        public List<Long> getMemoryUsage() { return memoryUsage; }
        public long getTotalMemoryIncrease() { return totalMemoryIncrease; }
    }
    
    /**
     * Resource snapshot data class.
     */
    public static class ResourceSnapshot {
        private final long timestamp;
        private final long usedMemory;
        private final long totalMemory;
        private final long maxMemory;
        private final long cpuTime;
        private final int threadCount;
        
        public ResourceSnapshot(long timestamp, long usedMemory, long totalMemory, long maxMemory,
                              long cpuTime, int threadCount) {
            this.timestamp = timestamp;
            this.usedMemory = usedMemory;
            this.totalMemory = totalMemory;
            this.maxMemory = maxMemory;
            this.cpuTime = cpuTime;
            this.threadCount = threadCount;
        }
        
        // Getters
        public long getTimestamp() { return timestamp; }
        public long getUsedMemory() { return usedMemory; }
        public long getTotalMemory() { return totalMemory; }
        public long getMaxMemory() { return maxMemory; }
        public long getCpuTime() { return cpuTime; }
        public int getThreadCount() { return threadCount; }
    }
    
    /**
     * Resource utilization result data class.
     */
    public static class ResourceUtilizationResult {
        private final DetectionType detectionType;
        private final List<ResourceSnapshot> snapshots;
        
        public ResourceUtilizationResult(DetectionType detectionType, List<ResourceSnapshot> snapshots) {
            this.detectionType = detectionType;
            this.snapshots = snapshots;
        }
        
        // Getters
        public DetectionType getDetectionType() { return detectionType; }
        public List<ResourceSnapshot> getSnapshots() { return snapshots; }
    }
    
    /**
     * Comprehensive benchmark report.
     */
    public static class BenchmarkReport {
        private final Map<DetectionType, BenchmarkResult> results = new HashMap<>();
        private final Map<DetectionType, String> errors = new HashMap<>();
        private final Map<String, Object> comparativeAnalysis = new HashMap<>();
        private final Map<String, Object> metrics = new HashMap<>();
        
        public void addResult(DetectionType detectionType, BenchmarkResult result) {
            results.put(detectionType, result);
        }
        
        public void addError(DetectionType detectionType, String error) {
            errors.put(detectionType, error);
        }
        
        public void addMetric(String name, Object value) {
            metrics.put(name, value);
        }
        
        public void generateComparativeAnalysis() {
            if (results.isEmpty()) {
                return;
            }
            
            // Find fastest and slowest detection types
            DetectionType fastest = results.entrySet().stream()
                .min(Map.Entry.comparingByValue(Comparator.comparing(BenchmarkResult::getAvgProcessingTime)))
                .map(Map.Entry::getKey)
                .orElse(null);
                
            DetectionType slowest = results.entrySet().stream()
                .max(Map.Entry.comparingByValue(Comparator.comparing(BenchmarkResult::getAvgProcessingTime)))
                .map(Map.Entry::getKey)
                .orElse(null);
            
            // Calculate average throughput
            double avgThroughput = results.values().stream()
                .mapToDouble(BenchmarkResult::getThroughput)
                .average()
                .orElse(0.0);
            
            comparativeAnalysis.put("fastest_detection_type", fastest);
            comparativeAnalysis.put("slowest_detection_type", slowest);
            comparativeAnalysis.put("average_throughput", avgThroughput);
            comparativeAnalysis.put("total_tests", results.size());
            comparativeAnalysis.put("total_errors", errors.size());
        }
        
        public String getSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Benchmark Report Summary:\n");
            summary.append("========================\n");
            
            for (Map.Entry<DetectionType, BenchmarkResult> entry : results.entrySet()) {
                BenchmarkResult result = entry.getValue();
                summary.append(String.format("%s: %.2fms avg, %.2f req/s, %.1f detections\n",
                    entry.getKey(), result.getAvgProcessingTime(), result.getThroughput(), result.getAvgDetectionCount()));
            }
            
            if (!errors.isEmpty()) {
                summary.append("\nErrors:\n");
                errors.forEach((type, error) -> summary.append(String.format("  %s: %s\n", type, error)));
            }
            
            return summary.toString();
        }
        
        // Getters
        public Map<DetectionType, BenchmarkResult> getResults() { return results; }
        public Map<DetectionType, String> getErrors() { return errors; }
        public Map<String, Object> getComparativeAnalysis() { return comparativeAnalysis; }
        public Map<String, Object> getMetrics() { return metrics; }
    }
} 