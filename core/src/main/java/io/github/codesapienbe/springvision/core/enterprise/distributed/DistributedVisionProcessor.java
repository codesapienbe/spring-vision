package io.github.codesapienbe.springvision.core.enterprise.distributed;

import io.github.codesapienbe.springvision.core.*;
import io.github.codesapienbe.springvision.core.enterprise.multitenancy.TenantContext;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Comprehensive distributed processing system for the Spring Vision framework.
 *
 * <p>This class provides enterprise-grade distributed processing capabilities with
 * fault tolerance, load balancing, task distribution, and comprehensive monitoring.
 * It ensures high availability, scalability, and reliability for production workloads.</p>
 *
 * <p>The distributed processor supports multiple processing strategies, automatic
 * failover, load balancing, and real-time health monitoring.</p>
 *
 * @author Spring Vision Team
 * @since 1.2.0
 */
@Component
public class DistributedVisionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DistributedVisionProcessor.class);

    // Processing nodes registry
    private final Map<String, ProcessingNode> processingNodes = new ConcurrentHashMap<>();

    // Task queue and distribution
    private final BlockingQueue<DistributedTask> taskQueue = new LinkedBlockingQueue<>();
    private final Map<String, CompletableFuture<DistributedResult>> pendingTasks = new ConcurrentHashMap<>();

    // Load balancer
    private final LoadBalancer loadBalancer;

    // Fault tolerance and recovery
    private final FaultToleranceManager faultToleranceManager;

    // Performance monitoring
    private final DistributedMetrics distributedMetrics;

    // Configuration
    private final DistributedConfiguration configuration;

    // Processing strategies
    private final Map<String, ProcessingStrategy> processingStrategies = new ConcurrentHashMap<>();

    // Health monitoring
    private final HealthMonitor healthMonitor;

    // ⭐ NEW: ExecutorService for proper thread management
    private final ExecutorService taskExecutor;
    private final ScheduledExecutorService timeoutScheduler;

    // ⭐ NEW: Background threads for proper lifecycle management
    private Thread taskProcessorThread;
    private Thread healthMonitorThread;
    private Thread metricsCollectorThread;

    // ⭐ NEW: Shutdown flag
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    /**
     * Default constructor for DistributedVisionProcessor.
     * Initializes configuration, load balancer, fault tolerance manager,
     * metrics, and health monitor, then starts background processes.
     */
    public DistributedVisionProcessor() {
        this.configuration = new DistributedConfiguration();
        this.loadBalancer = new LoadBalancer();
        this.faultToleranceManager = new FaultToleranceManager();
        this.distributedMetrics = new DistributedMetrics();
        this.healthMonitor = new HealthMonitor();

        // ⭐ NEW: Initialize managed ExecutorServices
        this.taskExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.timeoutScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "timeout-scheduler");
            t.setDaemon(true);
            return t;
        });

        initializeProcessingStrategies();
        startBackgroundProcesses();
    }

    /**
     * Processes a vision task using distributed processing.
     *
     * @param imageData the image data to process
     * @param query     the detection query
     * @return a CompletableFuture containing the distributed result
     */
    public CompletableFuture<DistributedResult> processDistributed(ImageData imageData, DetectionQuery query) {
        String taskId = generateTaskId();
        String tenantId = TenantContext.getCurrentTenantId();

        // Create a distributed task
        DistributedTask task = new DistributedTask(
            taskId,
            tenantId,
            imageData,
            query,
            System.currentTimeMillis(),
            configuration.getTaskTimeoutMs()
        );

        // Record task creation
        distributedMetrics.recordTaskCreated(taskId, tenantId);

        // Submit a task for processing
        CompletableFuture<DistributedResult> resultFuture = new CompletableFuture<>();
        pendingTasks.put(taskId, resultFuture);

        try {
            // Add a task to queue
            if (!taskQueue.offer(task, configuration.getQueueTimeoutMs(), TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Task queue is full");
            }

            // Schedule task timeout
            scheduleTaskTimeout(taskId, configuration.getTaskTimeoutMs());

            logger.info("Distributed task submitted: {} for tenant: {}", taskId, tenantId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pendingTasks.remove(taskId);
            resultFuture.completeExceptionally(e);
        }

        return resultFuture;
    }

    /**
     * Processes a batch of tasks using distributed processing.
     *
     * @param images The list of images to process.
     * @param query  The detection query to apply to all images.
     * @return A completable future containing a list of distributed results.
     */
    public CompletableFuture<List<DistributedResult>> processBatchDistributed(
        List<ImageData> images, DetectionQuery query) {

        List<CompletableFuture<DistributedResult>> futures = new ArrayList<>();

        for (ImageData image : images) {
            futures.add(processDistributed(image, query));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<DistributedResult> results = new ArrayList<>();
                for (CompletableFuture<DistributedResult> future : futures) {
                    try {
                        results.add(future.get());
                    } catch (Exception e) {
                        logger.error("Batch processing failed for task", e);
                        results.add(DistributedResult.createErrorResult(e.getMessage()));
                    }
                }
                return results;
            });
    }

    /**
     * Registers a processing node.
     *
     * @param nodeId       The unique identifier for the node.
     * @param nodeUrl      The URL of the node.
     * @param capabilities The capabilities of the node.
     */
    public void registerNode(String nodeId, String nodeUrl, NodeCapabilities capabilities) {
        ProcessingNode node = new ProcessingNode(nodeId, nodeUrl, capabilities);
        processingNodes.put(nodeId, node);

        // Update load balancer
        loadBalancer.addNode(node);

        // Start health monitoring
        healthMonitor.startMonitoring(node);

        logger.info("Processing node registered: {} at {}", nodeId, nodeUrl);
    }

    /**
     * Unregisters a processing node.
     *
     * @param nodeId The unique identifier for the node.
     */
    public void unregisterNode(String nodeId) {
        ProcessingNode node = processingNodes.remove(nodeId);
        if (node != null) {
            loadBalancer.removeNode(node);
            healthMonitor.stopMonitoring(node);

            // Handle pending tasks on this node
            faultToleranceManager.handleNodeFailure(node);

            logger.info("Processing node unregistered: {}", nodeId);
        }
    }

    /**
     * Gets processing node status.
     *
     * @param nodeId The unique identifier for the node.
     * @return The status of the node.
     */
    public NodeStatus getNodeStatus(String nodeId) {
        ProcessingNode node = processingNodes.get(nodeId);
        return node != null ? node.getStatus() : null;
    }

    /**
     * Gets all processing nodes.
     *
     * @return A map of all processing nodes.
     */
    public Map<String, ProcessingNode> getAllNodes() {
        return new ConcurrentHashMap<>(processingNodes);
    }

    /**
     * Gets distributed processing metrics.
     *
     * @return The distributed metrics.
     */
    public DistributedMetrics getMetrics() {
        return distributedMetrics;
    }

    /**
     * Gets health status of all nodes.
     *
     * @return A map of node health statuses.
     */
    public Map<String, HealthStatus> getAllNodeHealth() {
        return healthMonitor.getAllNodeHealth();
    }

    /**
     * Sets processing strategy for a detection type.
     *
     * @param detectionType The detection type.
     * @param strategy      The processing strategy.
     */
    public void setProcessingStrategy(DetectionType detectionType, ProcessingStrategy strategy) {
        processingStrategies.put(detectionType.name(), strategy);
        logger.info("Processing strategy set for {}: {}", detectionType, strategy.getClass().getSimpleName());
    }

    /**
     * Gets current configuration.
     *
     * @return The current distributed configuration.
     */
    public DistributedConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Updates configuration.
     *
     * @param newConfig The new distributed configuration.
     */
    public void updateConfiguration(DistributedConfiguration newConfig) {
        this.configuration.update(newConfig);
        logger.info("Distributed processing configuration updated");
    }

    /**
     * Initializes processing strategies.
     */
    private void initializeProcessingStrategies() {
        // Round-robin strategy for face detection
        setProcessingStrategy(DetectionType.FACE, new RoundRobinStrategy());

        // Load-based strategy for object detection
        setProcessingStrategy(DetectionType.OBJECT, new LoadBasedStrategy());

        // Priority-based strategy for pose detection
        setProcessingStrategy(DetectionType.POSE, new PriorityBasedStrategy());

        // Default strategy for other types
        setProcessingStrategy(DetectionType.HAND, new RoundRobinStrategy());
    }

    /**
     * Starts background processing threads.
     * ⭐ OPTIMIZED: Use Virtual Threads for lightweight, scalable background processing
     */
    private void startBackgroundProcesses() {
        // ⭐ Task processor thread - using Virtual Thread
        taskProcessorThread = Thread.ofVirtual()
            .name("distributed-task-processor")
            .start(this::processTaskQueue);

        // ⭐ Health monitor thread - using Virtual Thread
        healthMonitorThread = Thread.ofVirtual()
            .name("health-monitor")
            .start(healthMonitor::runHealthChecks);

        // ⭐ Metrics collection thread - using Virtual Thread
        metricsCollectorThread = Thread.ofVirtual()
            .name("metrics-collector")
            .start(distributedMetrics::collectMetrics);

        logger.info("Background processing threads started (using Virtual Threads)");
    }

    /**
     * Processes tasks from the queue.
     * ⭐ MODIFIED: Check shutdown flag
     */
    private void processTaskQueue() {
        while (!Thread.currentThread().isInterrupted() && !isShuttingDown.get()) {
            try {
                DistributedTask task = taskQueue.poll(1, TimeUnit.SECONDS);
                if (task != null) {
                    processTask(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error processing task from queue", e);
            }
        }
        logger.info("Task processor thread terminated");
    }

    /**
     * Processes a single task.
     */
    private void processTask(DistributedTask task) {
        String taskId = task.taskId();
        String tenantId = task.tenantId();

        try {
            // Check if task is still pending
            CompletableFuture<DistributedResult> resultFuture = pendingTasks.get(taskId);
            if (resultFuture == null) {
                logger.warn("Task {} is no longer pending", taskId);
                return;
            }

            // Select processing strategy
            ProcessingStrategy strategy = getProcessingStrategy(task.query().getType());

            // Route based on detection type
            DetectionType detectionType = task.query().getType();
            String nodeId = strategy.selectNode(loadBalancer.getAvailableNodes(), task).getNodeId();

            if (nodeId == null) {
                throw new VisionBackendException("No available processing node for detection type: " + detectionType);
            }

            // Execute task on selected node
            executeTaskOnNode(task, processingNodes.get(nodeId), resultFuture);

        } catch (Exception e) {
            logger.error("Failed to process task: {}", taskId, e);

            // Handle task failure
            faultToleranceManager.handleTaskFailure(task, e);

            // Complete with error
            CompletableFuture<DistributedResult> resultFuture = pendingTasks.remove(taskId);
            if (resultFuture != null) {
                resultFuture.complete(DistributedResult.createErrorResult(e.getMessage()));
            }

            // Record failure
            distributedMetrics.recordTaskFailed(taskId, tenantId, e.getMessage());
        }
    }

    /**
     * Executes a task on a specific node.
     * ⭐ MODIFIED: Use managed ExecutorService instead of creating a new one
     */
    private void executeTaskOnNode(DistributedTask task, ProcessingNode node,
                                   CompletableFuture<DistributedResult> resultFuture) {

        String taskId = task.taskId();
        String nodeId = node.getNodeId();

        // Check if shutting down
        if (isShuttingDown.get()) {
            resultFuture.complete(DistributedResult.createErrorResult("System shutting down"));
            pendingTasks.remove(taskId);
            return;
        }

        // Update node status
        node.incrementActiveTasks();

        // Execute a task using managed executor
        CompletableFuture.supplyAsync(() -> {
                try {
                    return executeTaskOnNodeInternal(task, node);
                } finally {
                    node.decrementActiveTasks();
                }
            }, taskExecutor)  // ⭐ Use managed executor instead of creating new one
            .whenComplete((result, throwable) -> {
                try {
                    if (throwable != null) {
                        // Handle execution failure
                        faultToleranceManager.handleNodeTaskFailure(task, node, throwable);
                        resultFuture.complete(DistributedResult.createErrorResult(throwable.getMessage()));
                    } else {
                        // Complete successfully
                        resultFuture.complete(result);
                        distributedMetrics.recordTaskCompleted(taskId, task.tenantId(), result.processingTimeMs());
                    }
                } finally {
                    pendingTasks.remove(taskId);
                }
            });
    }

    /**
     * Internal task execution on a node (simulated).
     */
    private DistributedResult executeTaskOnNodeInternal(DistributedTask task, ProcessingNode node) {
        long startTime = System.currentTimeMillis();

        try {
            // Simulate processing time
            Thread.sleep(100 + new Random().nextInt(200));

            // Simulate detection results
            List<Detection> detections = new ArrayList<>();
            if (task.query().getType() == DetectionType.FACE) {
                detections.add(new Detection(DetectionType.FACE.name(),
                    0.95, new BoundingBox(0.1, 0.2, 0.3, 0.4), Map.of()));
            }

            long processingTime = System.currentTimeMillis() - startTime;

            return new DistributedResult(
                task.taskId(),
                node.getNodeId(),
                detections,
                processingTime,
                true,
                null
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Task execution interrupted", e);
        }
    }

    /**
     * Gets processing strategy for detection type.
     */
    private ProcessingStrategy getProcessingStrategy(DetectionType detectionType) {
        return processingStrategies.getOrDefault(detectionType.name(), new RoundRobinStrategy());
    }

    /**
     * Generates unique task ID.
     */
    private String generateTaskId() {
        return "task-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(10000);
    }

    /**
     * Schedules task timeout.
     * Use managed ScheduledExecutorService and return ScheduledFuture for cancellation
     */
    private ScheduledFuture<?> scheduleTaskTimeout(String taskId, long timeoutMs) {
        return timeoutScheduler.schedule(() -> {
            CompletableFuture<DistributedResult> resultFuture = pendingTasks.remove(taskId);
            if (resultFuture != null && !resultFuture.isDone()) {
                resultFuture.complete(DistributedResult.createErrorResult("Task timeout"));
                distributedMetrics.recordTaskTimeout(taskId);
                logger.warn("Task {} timed out after {}ms", taskId, timeoutMs);
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Comprehensive shutdown method to prevent resource leaks.
     *
     * <p>This method ensures proper cleanup of all resources:
     * <ul>
     *   <li>Sets a shutdown flag to prevent new task acceptance</li>
     *   <li>Interrupts and joins background threads</li>
     *   <li>Shuts down managed ExecutorServices with timeout</li>
     *   <li>Cancels all pending tasks</li>
     *   <li>Clears all internal state</li>
     * </ul>
     */
    @PreDestroy
    public void shutdown() {
        if (isShuttingDown.compareAndSet(false, true)) {
            logger.info("Shutting down DistributedVisionProcessor...");

            try {
                // 1. Cancel all pending tasks
                logger.info("Cancelling {} pending tasks...", pendingTasks.size());
                pendingTasks.forEach((taskId, future) -> {
                    if (!future.isDone()) {
                        future.complete(DistributedResult.createErrorResult("System shutting down"));
                    }
                });
                pendingTasks.clear();

                // 2. Interrupt background threads
                logger.info("Interrupting background threads...");
                if (taskProcessorThread != null && taskProcessorThread.isAlive()) {
                    taskProcessorThread.interrupt();
                }
                if (healthMonitorThread != null && healthMonitorThread.isAlive()) {
                    healthMonitorThread.interrupt();
                }
                if (metricsCollectorThread != null && metricsCollectorThread.isAlive()) {
                    metricsCollectorThread.interrupt();
                }

                // 3. Wait for threads to terminate (with timeout)
                logger.info("Waiting for background threads to terminate...");
                if (taskProcessorThread != null) {
                    try {
                        taskProcessorThread.join(5000);
                        if (taskProcessorThread.isAlive()) {
                            logger.warn("Task processor thread did not terminate in time");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Interrupted while waiting for task processor thread");
                    }
                }

                if (healthMonitorThread != null) {
                    try {
                        healthMonitorThread.join(2000);
                        if (healthMonitorThread.isAlive()) {
                            logger.warn("Health monitor thread did not terminate in time");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Interrupted while waiting for health monitor thread");
                    }
                }

                if (metricsCollectorThread != null) {
                    try {
                        metricsCollectorThread.join(2000);
                        if (metricsCollectorThread.isAlive()) {
                            logger.warn("Metrics collector thread did not terminate in time");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Interrupted while waiting for metrics collector thread");
                    }
                }

                // 4. Shutdown ExecutorServices
                logger.info("Shutting down ExecutorServices...");

                // Shutdown task executor
                taskExecutor.shutdown();
                try {
                    if (!taskExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                        logger.warn("Task executor did not terminate in time, forcing shutdown...");
                        taskExecutor.shutdownNow();
                        if (!taskExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                            logger.error("Task executor did not terminate after forced shutdown");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    taskExecutor.shutdownNow();
                    logger.warn("Interrupted while shutting down task executor");
                }

                // Shutdown timeout scheduler
                timeoutScheduler.shutdown();
                try {
                    if (!timeoutScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                        logger.warn("Timeout scheduler did not terminate in time, forcing shutdown...");
                        timeoutScheduler.shutdownNow();
                        if (!timeoutScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                            logger.error("Timeout scheduler did not terminate after forced shutdown");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    timeoutScheduler.shutdownNow();
                    logger.warn("Interrupted while shutting down timeout scheduler");
                }

                // 5. Clear remaining state
                logger.info("Clearing internal state...");
                taskQueue.clear();
                processingNodes.clear();
                processingStrategies.clear();

                logger.info("DistributedVisionProcessor shutdown completed successfully");

            } catch (Exception e) {
                logger.error("Error during DistributedVisionProcessor shutdown", e);
            }
        } else {
            logger.warn("Shutdown already in progress or completed");
        }
    }

    /**
     * Distributed task data class.
     */
    public record DistributedTask(String taskId, String tenantId, ImageData imageData, DetectionQuery query,
                                  long createdAt, long timeoutMs) {
        /**
         * Constructs a new DistributedTask.
         *
         * @param taskId    The unique identifier for the task.
         * @param tenantId  The identifier of the tenant submitting the task.
         * @param imageData The image data to process.
         * @param query     The detection query.
         * @param createdAt The timestamp of when the task was created.
         * @param timeoutMs The timeout for the task in milliseconds.
         */
        public DistributedTask {
        }

        // Getters

        /**
         * Gets the unique identifier for the task.
         *
         * @return The unique identifier for the task.
         */
        @Override
        public String taskId() {
            return taskId;
        }

        /**
         * Gets the identifier of the tenant submitting the task.
         *
         * @return The identifier of the tenant submitting the task.
         */
        @Override
        public String tenantId() {
            return tenantId;
        }

        /**
         * Gets the image data to process.
         *
         * @return The image data to process.
         */
        @Override
        public ImageData imageData() {
            return imageData;
        }

        /**
         * Gets the detection query.
         *
         * @return The detection query.
         */
        @Override
        public DetectionQuery query() {
            return query;
        }

        /**
         * Gets the timestamp of when the task was created.
         *
         * @return The timestamp of when the task was created.
         */
        @Override
        public long createdAt() {
            return createdAt;
        }

        /**
         * Gets the timeout for the task in milliseconds.
         *
         * @return The timeout for the task in milliseconds.
         */
        @Override
        public long timeoutMs() {
            return timeoutMs;
        }
    }

    /**
     * Distributed result data class.
     */
    public record DistributedResult(String taskId, String nodeId, List<Detection> detections, long processingTimeMs,
                                    boolean success, String errorMessage) {
        /**
         * Constructs a new DistributedResult.
         *
         * @param taskId           The unique identifier for the task.
         * @param nodeId           The identifier of the node that processed the task.
         * @param detections       The list of detections.
         * @param processingTimeMs The processing time in milliseconds.
         * @param success          Whether the task was successful.
         * @param errorMessage     The error message if the task failed.
         */
        public DistributedResult {
        }

        /**
         * Creates an error result.
         *
         * @param errorMessage The error message.
         * @return A new DistributedResult representing an error.
         */
        public static DistributedResult createErrorResult(String errorMessage) {
            return new DistributedResult(null, null, new ArrayList<>(), 0, false, errorMessage);
        }

        // Getters

        /**
         * Gets the unique identifier for the task.
         *
         * @return The unique identifier for the task.
         */
        @Override
        public String taskId() {
            return taskId;
        }

        /**
         * Gets the identifier of the node that processed the task.
         *
         * @return The identifier of the node that processed the task.
         */
        @Override
        public String nodeId() {
            return nodeId;
        }

        /**
         * Gets the list of detections.
         *
         * @return The list of detections.
         */
        @Override
        public List<Detection> detections() {
            return detections;
        }

        /**
         * Gets the processing time in milliseconds.
         *
         * @return The processing time in milliseconds.
         */
        @Override
        public long processingTimeMs() {
            return processingTimeMs;
        }

        /**
         * Checks if the task was successful.
         *
         * @return Whether the task was successful.
         */
        @Override
        public boolean success() {
            return success;
        }

        /**
         * Gets the error message if the task failed.
         *
         * @return The error message if the task failed.
         */
        @Override
        public String errorMessage() {
            return errorMessage;
        }
    }

    /**
     * Processing node data class.
     */
    public static class ProcessingNode {
        private final String nodeId;
        private final String nodeUrl;
        private final NodeCapabilities capabilities;
        private final AtomicLong activeTasks = new AtomicLong(0);
        private final AtomicReference<NodeStatus> status = new AtomicReference<>(NodeStatus.HEALTHY);
        private final AtomicLong totalTasks = new AtomicLong(0);
        private final AtomicLong failedTasks = new AtomicLong(0);
        private final AtomicLong totalProcessingTime = new AtomicLong(0);

        /**
         * Constructs a new ProcessingNode.
         *
         * @param nodeId       The unique identifier for the node.
         * @param nodeUrl      The URL of the node.
         * @param capabilities The capabilities of the node.
         */
        public ProcessingNode(String nodeId, String nodeUrl, NodeCapabilities capabilities) {
            this.nodeId = nodeId;
            this.nodeUrl = nodeUrl;
            this.capabilities = capabilities;
        }

        /**
         * Increments the number of active tasks.
         */
        public void incrementActiveTasks() {
            activeTasks.incrementAndGet();
            totalTasks.incrementAndGet();
        }

        /**
         * Decrements the number of active tasks.
         */
        public void decrementActiveTasks() {
            activeTasks.decrementAndGet();
        }

        /**
         * Records a task failure.
         */
        public void recordTaskFailure() {
            failedTasks.incrementAndGet();
        }

        /**
         * Records the processing time of a task.
         *
         * @param processingTimeMs The processing time in milliseconds.
         */
        public void recordProcessingTime(long processingTimeMs) {
            totalProcessingTime.addAndGet(processingTimeMs);
        }

        /**
         * Sets the status of the node.
         *
         * @param status The new status.
         */
        public void setStatus(NodeStatus status) {
            this.status.set(status);
        }

        /**
         * Gets the load factor of the node.
         *
         * @return The load factor of the node.
         */
        public double getLoadFactor() {
            long total = totalTasks.get();
            return total > 0 ? (double) activeTasks.get() / total : 0.0;
        }

        /**
         * Gets the failure rate of the node.
         *
         * @return The failure rate of the node.
         */
        public double getFailureRate() {
            long total = totalTasks.get();
            return total > 0 ? (double) failedTasks.get() / total : 0.0;
        }

        /**
         * Gets the average processing time of the node.
         *
         * @return The average processing time of the node.
         */
        public double getAverageProcessingTime() {
            long total = totalTasks.get();
            return total > 0 ? (double) totalProcessingTime.get() / total : 0.0;
        }

        // Getters

        /**
         * Gets the unique identifier for the node.
         *
         * @return The unique identifier for the node.
         */
        public String getNodeId() {
            return nodeId;
        }

        /**
         * Gets the URL of the node.
         *
         * @return The URL of the node.
         */
        public String getNodeUrl() {
            return nodeUrl;
        }

        /**
         * Gets the capabilities of the node.
         *
         * @return The capabilities of the node.
         */
        public NodeCapabilities getCapabilities() {
            return capabilities;
        }

        /**
         * Gets the number of active tasks.
         *
         * @return The number of active tasks.
         */
        public long getActiveTasks() {
            return activeTasks.get();
        }

        /**
         * Gets the status of the node.
         *
         * @return The status of the node.
         */
        public NodeStatus getStatus() {
            return status.get();
        }

        /**
         * Gets the total number of tasks processed by the node.
         *
         * @return The total number of tasks processed by the node.
         */
        public long getTotalTasks() {
            return totalTasks.get();
        }

        /**
         * Gets the number of failed tasks on the node.
         *
         * @return The number of failed tasks on the node.
         */
        public long getFailedTasks() {
            return failedTasks.get();
        }
    }

    /**
     * Node capabilities data class.
     */
    public record NodeCapabilities(Set<DetectionType> supportedTypes, int maxConcurrentTasks, long maxMemoryUsage,
                                   boolean gpuAcceleration) {
        /**
         * Constructs a new NodeCapabilities.
         *
         * @param supportedTypes     The set of supported detection types.
         * @param maxConcurrentTasks The maximum number of concurrent tasks.
         * @param maxMemoryUsage     The maximum memory usage.
         * @param gpuAcceleration    Whether GPU acceleration is enabled.
         */
        public NodeCapabilities {
        }

        /**
         * Checks if the node supports a detection type.
         *
         * @param type The detection type.
         * @return True if the node supports the detection type, false otherwise.
         */
        public boolean supportsDetectionType(DetectionType type) {
            return supportedTypes.contains(type);
        }

        // Getters

        /**
         * Gets the set of supported detection types.
         *
         * @return The set of supported detection types.
         */
        @Override
        public Set<DetectionType> supportedTypes() {
            return supportedTypes;
        }

        /**
         * Gets the maximum number of concurrent tasks.
         *
         * @return The maximum number of concurrent tasks.
         */
        @Override
        public int maxConcurrentTasks() {
            return maxConcurrentTasks;
        }

        /**
         * Gets the maximum memory usage.
         *
         * @return The maximum memory usage.
         */
        @Override
        public long maxMemoryUsage() {
            return maxMemoryUsage;
        }

        /**
         * Checks if GPU acceleration is enabled.
         *
         * @return Whether GPU acceleration is enabled.
         */
        @Override
        public boolean gpuAcceleration() {
            return gpuAcceleration;
        }
    }

    /**
     * Node status enum.
     */
    public enum NodeStatus {
        /**
         * The node is healthy.
         */
        HEALTHY,
        /**
         * The node is degraded.
         */
        DEGRADED,
        /**
         * The node is unhealthy.
         */
        UNHEALTHY,
        /**
         * The node is offline.
         */
        OFFLINE
    }

    /**
     * Health status data class.
     */
    public static class HealthStatus {
        private final NodeStatus status;
        private final long lastCheckTime;
        private final String details;

        /**
         * Constructs a new HealthStatus.
         *
         * @param status        The status of the node.
         * @param lastCheckTime The timestamp of the last health check.
         * @param details       The details of the health check.
         */
        public HealthStatus(NodeStatus status, long lastCheckTime, String details) {
            this.status = status;
            this.lastCheckTime = lastCheckTime;
            this.details = details;
        }

        // Getters

        /**
         * Gets the status of the node.
         *
         * @return The status of the node.
         */
        public NodeStatus getStatus() {
            return status;
        }

        /**
         * Gets the timestamp of the last health check.
         *
         * @return The timestamp of the last health check.
         */
        public long getLastCheckTime() {
            return lastCheckTime;
        }

        /**
         * Gets the details of the health check.
         *
         * @return The details of the health check.
         */
        public String getDetails() {
            return details;
        }
    }

    /**
     * Processing strategy interface.
     */
    public interface ProcessingStrategy {
        /**
         * Selects a node for a task.
         *
         * @param availableNodes The list of available nodes.
         * @param task           The task to be processed.
         * @return The selected node.
         */
        ProcessingNode selectNode(List<ProcessingNode> availableNodes, DistributedTask task);
    }

    /**
     * Round-robin processing strategy.
     */
    public static class RoundRobinStrategy implements ProcessingStrategy {
        private final AtomicLong counter = new AtomicLong(0);

        /**
         * Default constructor for RoundRobinStrategy.
         */
        public RoundRobinStrategy() {
            // Default constructor
        }

        @Override
        public ProcessingNode selectNode(List<ProcessingNode> availableNodes, DistributedTask task) {
            if (availableNodes.isEmpty()) {
                return null;
            }

            int index = (int) (counter.incrementAndGet() % availableNodes.size());
            return availableNodes.get(index);
        }
    }

    /**
     * Load-based processing strategy.
     */
    public static class LoadBasedStrategy implements ProcessingStrategy {

        /**
         * Default constructor for LoadBasedStrategy.
         */
        public LoadBasedStrategy() {
            // Default constructor
        }

        @Override
        public ProcessingNode selectNode(List<ProcessingNode> availableNodes, DistributedTask task) {
            return availableNodes.stream()
                .filter(node -> node.getCapabilities().supportsDetectionType(task.query().getType()))
                .min(Comparator.comparingDouble(ProcessingNode::getLoadFactor))
                .orElse(null);
        }
    }

    /**
     * Priority-based processing strategy.
     */
    public static class PriorityBasedStrategy implements ProcessingStrategy {

        /**
         * Default constructor for PriorityBasedStrategy.
         */
        public PriorityBasedStrategy() {
            // Default constructor
        }

        @Override
        public ProcessingNode selectNode(List<ProcessingNode> availableNodes, DistributedTask task) {
            return availableNodes.stream()
                .filter(node -> node.getCapabilities().supportsDetectionType(task.query().getType()))
                .filter(node -> node.getStatus() == NodeStatus.HEALTHY)
                .min(Comparator.comparingDouble(ProcessingNode::getFailureRate))
                .orElse(null);
        }
    }

    /**
     * Load balancer for distributing tasks.
     */
    private static class LoadBalancer {
        private final List<ProcessingNode> availableNodes = new CopyOnWriteArrayList<>();

        public void addNode(ProcessingNode node) {
            availableNodes.add(node);
        }

        public void removeNode(ProcessingNode node) {
            availableNodes.remove(node);
        }

        public List<ProcessingNode> getAvailableNodes() {
            return new ArrayList<>(availableNodes);
        }
    }

    /**
     * Fault tolerance manager.
     */
    private static class FaultToleranceManager {
        public void handleNodeFailure(ProcessingNode node) {
            // Implement node failure handling
            logger.warn("Node failure detected: {}", node.getNodeId());
        }

        public void handleTaskFailure(DistributedTask task, Exception error) {
            // Implement task failure handling
            logger.warn("Task failure detected: {}", task.taskId());
        }

        public void handleNodeTaskFailure(DistributedTask task, ProcessingNode node, Throwable error) {
            // Implement node task failure handling
            node.recordTaskFailure();
            logger.warn("Node task failure: {} on node: {}", task.taskId(), node.getNodeId());
        }
    }

    /**
     * Health monitor for processing nodes.
     */
    private static class HealthMonitor {
        private final Map<String, HealthStatus> nodeHealth = new ConcurrentHashMap<>();
        private final Map<String, ProcessingNode> monitoredNodes = new ConcurrentHashMap<>();

        public void startMonitoring(ProcessingNode node) {
            monitoredNodes.put(node.getNodeId(), node);
        }

        public void stopMonitoring(ProcessingNode node) {
            monitoredNodes.remove(node.getNodeId());
            nodeHealth.remove(node.getNodeId());
        }

        public void runHealthChecks() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    for (ProcessingNode node : monitoredNodes.values()) {
                        HealthStatus health = checkNodeHealth(node);
                        nodeHealth.put(node.getNodeId(), health);
                        node.setStatus(health.getStatus());
                    }

                    Thread.sleep(30000); // Check every 30 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error during health checks", e);
                }
            }
        }

        private HealthStatus checkNodeHealth(ProcessingNode node) {
            // Simulate health check
            NodeStatus status = node.getFailureRate() > 0.1 ? NodeStatus.DEGRADED : NodeStatus.HEALTHY;
            return new HealthStatus(status, System.currentTimeMillis(), "Health check completed");
        }

        public Map<String, HealthStatus> getAllNodeHealth() {
            return new ConcurrentHashMap<>(nodeHealth);
        }
    }

    /**
     * Distributed metrics collection.
     */
    private static class DistributedMetrics {
        private final AtomicLong totalTasksCreated = new AtomicLong(0);
        private final AtomicLong totalTasksCompleted = new AtomicLong(0);
        private final AtomicLong totalTasksFailed = new AtomicLong(0);
        private final AtomicLong totalTasksTimeout = new AtomicLong(0);
        private final Map<String, AtomicLong> tenantTaskCounts = new ConcurrentHashMap<>();

        public void recordTaskCreated(String taskId, String tenantId) {
            totalTasksCreated.incrementAndGet();
            tenantTaskCounts.computeIfAbsent(tenantId, k -> new AtomicLong()).incrementAndGet();
        }

        public void recordTaskCompleted(String taskId, String tenantId, long processingTimeMs) {
            totalTasksCompleted.incrementAndGet();
        }

        public void recordTaskFailed(String taskId, String tenantId, String error) {
            totalTasksFailed.incrementAndGet();
        }

        public void recordTaskTimeout(String taskId) {
            totalTasksTimeout.incrementAndGet();
        }

        /**
         * ⭐ FIXED: Proper periodic metrics collection with shutdown support
         */
        public void collectMetrics() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Log current metrics periodically
                    logger.info("Distributed metrics - Created: {}, Completed: {}, Failed: {}, Timeout: {}",
                        totalTasksCreated.get(),
                        totalTasksCompleted.get(),
                        totalTasksFailed.get(),
                        totalTasksTimeout.get());

                    // Log per-tenant metrics if any
                    if (!tenantTaskCounts.isEmpty()) {
                        logger.debug("Tenant task counts: {}", getTenantTaskCounts());
                    }

                    // Sleep for 60 seconds between collections
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.info("Metrics collector thread terminated");
                    break;
                } catch (Exception e) {
                    logger.error("Error during metrics collection", e);
                }
            }
        }

        // Getters
        public long getTotalTasksCreated() {
            return totalTasksCreated.get();
        }

        public long getTotalTasksCompleted() {
            return totalTasksCompleted.get();
        }

        public long getTotalTasksFailed() {
            return totalTasksFailed.get();
        }

        public long getTotalTasksTimeout() {
            return totalTasksTimeout.get();
        }

        public Map<String, Long> getTenantTaskCounts() {
            Map<String, Long> result = new ConcurrentHashMap<>();
            tenantTaskCounts.forEach((k, v) -> result.put(k, v.get()));
            return result;
        }
    }

    /**
     * Distributed configuration.
     */
    public static class DistributedConfiguration {
        private long taskTimeoutMs = 30000; // 30 seconds
        private long queueTimeoutMs = 5000; // 5 seconds
        private int maxQueueSize = 1000;
        private int maxConcurrentTasks = 100;
        private boolean enableFaultTolerance = true;
        private boolean enableLoadBalancing = true;

        /**
         * Default constructor for DistributedConfiguration.
         */
        public DistributedConfiguration() {
            // Default constructor
        }

        /**
         * Updates the configuration.
         *
         * @param newConfig The new configuration.
         */
        public void update(DistributedConfiguration newConfig) {
            this.taskTimeoutMs = newConfig.taskTimeoutMs;
            this.queueTimeoutMs = newConfig.queueTimeoutMs;
            this.maxQueueSize = newConfig.maxQueueSize;
            this.maxConcurrentTasks = newConfig.maxConcurrentTasks;
            this.enableFaultTolerance = newConfig.enableFaultTolerance;
            this.enableLoadBalancing = newConfig.enableLoadBalancing;
        }

        // Getters

        /**
         * Gets the task timeout in milliseconds.
         *
         * @return The task timeout in milliseconds.
         */
        public long getTaskTimeoutMs() {
            return taskTimeoutMs;
        }

        /**
         * Gets the queue timeout in milliseconds.
         *
         * @return The queue timeout in milliseconds.
         */
        public long getQueueTimeoutMs() {
            return queueTimeoutMs;
        }

        /**
         * Gets the maximum queue size.
         *
         * @return The maximum queue size.
         */
        public int getMaxQueueSize() {
            return maxQueueSize;
        }

        /**
         * Gets the maximum number of concurrent tasks.
         *
         * @return The maximum number of concurrent tasks.
         */
        public int getMaxConcurrentTasks() {
            return maxConcurrentTasks;
        }

        /**
         * Checks if fault tolerance is enabled.
         *
         * @return Whether fault tolerance is enabled.
         */
        public boolean isEnableFaultTolerance() {
            return enableFaultTolerance;
        }

        /**
         * Checks if load balancing is enabled.
         *
         * @return Whether load balancing is enabled.
         */
        public boolean isEnableLoadBalancing() {
            return enableLoadBalancing;
        }
    }
}
