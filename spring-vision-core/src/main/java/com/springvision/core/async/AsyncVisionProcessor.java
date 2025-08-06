package com.springvision.core.async;

import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.DetectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.Objects;

/**
 * Asynchronous processor for vision operations.
 *
 * <p>This class provides asynchronous processing capabilities for vision
 * operations, allowing non-blocking execution of detection tasks. It supports
 * task tracking, progress monitoring, and result handling through callbacks.</p>
 *
 * <p>The processor uses a configurable thread pool and provides methods for
 * submitting tasks, monitoring progress, and retrieving results.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * AsyncVisionProcessor processor = new AsyncVisionProcessor();
 *
 * // Submit a task for asynchronous processing
 * CompletableFuture<VisionResult> future = processor.processAsync(
 *     imageData,
 *     DetectionType.FACE,
 *     Map.of("confidence", 0.8)
 * );
 *
 * // Handle the result
 * future.thenAccept(result -> {
 *     System.out.println("Detection completed: " + result.detectionCount());
 * });
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionTask
 * @see TaskProgress
 * @see TaskResult
 */
public class AsyncVisionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncVisionProcessor.class);

    private final Executor executor;
    private final Map<String, VisionTask> activeTasks;
    private final VisionTaskExecutor taskExecutor;
    private final AtomicInteger taskCounter;

    /**
     * Creates a new AsyncVisionProcessor with default settings.
     */
    public AsyncVisionProcessor() {
        this(Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new VisionThreadFactory()
        ));
    }

    /**
     * Creates a new AsyncVisionProcessor with the specified executor.
     *
     * @param executor the executor to use for task processing
     */
    public AsyncVisionProcessor(Executor executor) {
        this.executor = Objects.requireNonNull(executor, "Executor must not be null");
        this.activeTasks = new ConcurrentHashMap<>();
        this.taskExecutor = new VisionTaskExecutor();
        this.taskCounter = new AtomicInteger(0);

        logger.info("AsyncVisionProcessor initialized with executor: {}", executor.getClass().getSimpleName());
    }

    /**
     * Processes an image asynchronously with the specified detection type.
     *
     * @param imageData the image data to process
     * @param detectionType the detection type to perform
     * @param parameters the processing parameters
     * @return a CompletableFuture that will complete with the vision result
     */
    public CompletableFuture<VisionResult> processAsync(
            ImageData imageData,
            DetectionType detectionType,
            Map<String, Object> parameters) {

        return processAsync(imageData, detectionType, parameters, null);
    }

    /**
     * Processes an image asynchronously with progress monitoring.
     *
     * @param imageData the image data to process
     * @param detectionType the detection type to perform
     * @param parameters the processing parameters
     * @param progressCallback the progress callback
     * @return a CompletableFuture that will complete with the vision result
     */
    public CompletableFuture<VisionResult> processAsync(
            ImageData imageData,
            DetectionType detectionType,
            Map<String, Object> parameters,
            Consumer<TaskProgress> progressCallback) {

        Objects.requireNonNull(imageData, "Image data must not be null");
        Objects.requireNonNull(detectionType, "Detection type must not be null");

        String taskId = generateTaskId();
        VisionTask task = new VisionTask(taskId, imageData, detectionType, parameters);

        activeTasks.put(taskId, task);

        CompletableFuture<VisionResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Starting async vision task: {}", taskId);

                // Update progress
                updateProgress(task, TaskProgress.started(taskId));
                if (progressCallback != null) {
                    progressCallback.accept(TaskProgress.started(taskId));
                }

                // Execute the task
                VisionResult result = taskExecutor.executeTask(task);

                // Update progress
                updateProgress(task, TaskProgress.completed(taskId, result));
                if (progressCallback != null) {
                    progressCallback.accept(TaskProgress.completed(taskId, result));
                }

                logger.debug("Completed async vision task: {}", taskId);
                return result;

            } catch (Exception e) {
                logger.error("Error processing async vision task: {}", taskId, e);

                // Update progress
                TaskProgress failedProgress = TaskProgress.failed(taskId, e);
                updateProgress(task, failedProgress);
                if (progressCallback != null) {
                    progressCallback.accept(failedProgress);
                }

                throw new RuntimeException("Vision processing failed", e);

            } finally {
                activeTasks.remove(taskId);
            }
        }, executor);

        return future;
    }

    /**
     * Processes multiple detection types asynchronously.
     *
     * @param imageData the image data to process
     * @param detectionTypes the detection types to perform
     * @param parameters the processing parameters
     * @return a CompletableFuture that will complete with a list of vision results
     */
    public CompletableFuture<List<VisionResult>> processMultipleAsync(
            ImageData imageData,
            List<DetectionType> detectionTypes,
            Map<String, Object> parameters) {

        Objects.requireNonNull(imageData, "Image data must not be null");
        Objects.requireNonNull(detectionTypes, "Detection types must not be null");

        List<CompletableFuture<VisionResult>> futures = detectionTypes.stream()
            .map(detectionType -> processAsync(imageData, detectionType, parameters))
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }

    /**
     * Gets the current progress of a task.
     *
     * @param taskId the task identifier
     * @return the task progress, or null if task not found
     */
    public TaskProgress getTaskProgress(String taskId) {
        VisionTask task = activeTasks.get(taskId);
        return task != null ? task.getProgress() : null;
    }

    /**
     * Gets all active tasks.
     *
     * @return a map of task IDs to active tasks
     */
    public Map<String, VisionTask> getActiveTasks() {
        return Map.copyOf(activeTasks);
    }

    /**
     * Gets the number of active tasks.
     *
     * @return the number of active tasks
     */
    public int getActiveTaskCount() {
        return activeTasks.size();
    }

    /**
     * Cancels a task if it's still running.
     *
     * @param taskId the task identifier
     * @return true if the task was cancelled, false if not found or already completed
     */
    public boolean cancelTask(String taskId) {
        VisionTask task = activeTasks.get(taskId);
        if (task != null) {
            task.cancel();
            activeTasks.remove(taskId);
            logger.info("Cancelled vision task: {}", taskId);
            return true;
        }
        return false;
    }

    /**
     * Cancels all active tasks.
     */
    public void cancelAllTasks() {
        activeTasks.values().forEach(VisionTask::cancel);
        activeTasks.clear();
        logger.info("Cancelled all active vision tasks");
    }

    /**
     * Shuts down the processor and waits for all tasks to complete.
     */
    public void shutdown() {
        logger.info("Shutting down AsyncVisionProcessor");

        // Cancel all active tasks
        cancelAllTasks();

        // Shutdown executor if it's an ExecutorService
        if (executor instanceof java.util.concurrent.ExecutorService executorService) {
            executorService.shutdown();
        }
    }

    /**
     * Shuts down the processor immediately, interrupting running tasks.
     */
    public void shutdownNow() {
        logger.info("Shutting down AsyncVisionProcessor immediately");

        // Cancel all active tasks
        cancelAllTasks();

        // Shutdown executor if it's an ExecutorService
        if (executor instanceof java.util.concurrent.ExecutorService executorService) {
            executorService.shutdownNow();
        }
    }

    /**
     * Generates a unique task identifier.
     *
     * @return a unique task identifier
     */
    private String generateTaskId() {
        return String.format("vision-task-%d-%s",
                           taskCounter.incrementAndGet(),
                           UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * Updates the progress of a task.
     *
     * @param task the task to update
     * @param progress the new progress
     */
    private void updateProgress(VisionTask task, TaskProgress progress) {
        task.setProgress(progress);
    }

    /**
     * Thread factory for vision processing threads.
     */
    private static class VisionThreadFactory implements ThreadFactory {
        private final AtomicInteger threadCounter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "vision-processor-" + threadCounter.getAndIncrement());
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }

    /**
     * Executor for vision tasks.
     */
    private static class VisionTaskExecutor {

        private static final Logger logger = LoggerFactory.getLogger(VisionTaskExecutor.class);

        /**
         * Executes a vision task.
         *
         * @param task the task to execute
         * @return the vision result
         */
        public VisionResult executeTask(VisionTask task) {
            // This is a placeholder implementation
            // In a real implementation, this would delegate to the actual vision backend

            logger.debug("Executing vision task: {}", task.getTaskId());

            // Simulate processing time
            try {
                Thread.sleep(100 + (long) (Math.random() * 900)); // 100-1000ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Task interrupted", e);
            }

            // Return a mock result
            return new VisionResult(
                task.getDetectionType(),
                List.of(),
                0.0,
                System.currentTimeMillis() - task.getStartTime(),
                java.time.Instant.now(),
                Map.of("taskId", task.getTaskId())
            );
        }
    }
}
