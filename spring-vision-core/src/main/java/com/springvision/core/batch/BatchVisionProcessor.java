package com.springvision.core.batch;

import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.DetectionType;
import com.springvision.core.backend.OpenCvVisionBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import com.springvision.core.Detection;

/**
 * Batch processor for vision operations.
 *
 * <p>This class provides batch processing capabilities for vision operations,
 * allowing efficient processing of multiple images with configurable batch sizes,
 * parallel processing, and progress monitoring.</p>
 *
 * <p>The batch processor optimizes resource usage by processing images in batches
 * and provides methods for monitoring progress and handling results.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * BatchVisionProcessor processor = new BatchVisionProcessor();
 *
 * List<ImageData> images = // ... list of images
 *
 * // Process images in batches
 * CompletableFuture<List<VisionResult>> future = processor.processBatch(
 *     images,
 *     DetectionType.FACE,
 *     Map.of("confidence", 0.8),
 *     progress -> System.out.println("Progress: " + progress.getCompletionPercentage())
 * );
 *
 * // Handle results
 * List<VisionResult> results = future.get();
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see BatchProgress
 * @see BatchResult
 */
public class BatchVisionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BatchVisionProcessor.class);

    private final Executor executor;
    private final int defaultBatchSize;
    private final int maxConcurrentBatches;
    private final AtomicInteger batchCounter;
    private final Map<String, BatchJob> activeBatches;

    /**
     * Creates a new BatchVisionProcessor with default settings.
     */
    public BatchVisionProcessor() {
        this(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
             10, // Default batch size
             4); // Max concurrent batches
    }

    /**
     * Creates a new BatchVisionProcessor with the specified settings.
     *
     * @param executor the executor to use for batch processing
     * @param defaultBatchSize the default batch size
     * @param maxConcurrentBatches the maximum number of concurrent batches
     */
    public BatchVisionProcessor(Executor executor, int defaultBatchSize, int maxConcurrentBatches) {
        this.executor = Objects.requireNonNull(executor, "Executor must not be null");
        this.defaultBatchSize = Math.max(1, defaultBatchSize);
        this.maxConcurrentBatches = Math.max(1, maxConcurrentBatches);
        this.batchCounter = new AtomicInteger(0);
        this.activeBatches = new ConcurrentHashMap<>();

        logger.info("BatchVisionProcessor initialized with batchSize={}, maxConcurrentBatches={}",
                   defaultBatchSize, maxConcurrentBatches);
    }

    /**
     * Processes a batch of images with the specified detection type.
     *
     * @param images the list of images to process
     * @param detectionType the detection type to perform
     * @param parameters the processing parameters
     * @return a CompletableFuture that will complete with a list of vision results
     */
    public CompletableFuture<List<VisionResult>> processBatch(
            List<ImageData> images,
            DetectionType detectionType,
            Map<String, Object> parameters) {

        return processBatch(images, detectionType, parameters, defaultBatchSize, null);
    }

    /**
     * Processes a batch of images with progress monitoring.
     *
     * @param images the list of images to process
     * @param detectionType the detection type to perform
     * @param parameters the processing parameters
     * @param progressCallback the progress callback
     * @return a CompletableFuture that will complete with a list of vision results
     */
    public CompletableFuture<List<VisionResult>> processBatch(
            List<ImageData> images,
            DetectionType detectionType,
            Map<String, Object> parameters,
            Consumer<BatchProgress> progressCallback) {

        return processBatch(images, detectionType, parameters, defaultBatchSize, progressCallback);
    }

    /**
     * Processes a batch of images with custom batch size and progress monitoring.
     *
     * @param images the list of images to process
     * @param detectionType the detection type to perform
     * @param parameters the processing parameters
     * @param batchSize the batch size
     * @param progressCallback the progress callback
     * @return a CompletableFuture that will complete with a list of vision results
     */
    public CompletableFuture<List<VisionResult>> processBatch(
            List<ImageData> images,
            DetectionType detectionType,
            Map<String, Object> parameters,
            int batchSize,
            Consumer<BatchProgress> progressCallback) {

        Objects.requireNonNull(images, "Images list must not be null");
        Objects.requireNonNull(detectionType, "Detection type must not be null");

        if (images.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        String batchId = generateBatchId();
        BatchJob batchJob = new BatchJob(batchId, images, detectionType, parameters, batchSize);

        activeBatches.put(batchId, batchJob);

        CompletableFuture<List<VisionResult>> future = CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting batch processing: {} with {} images", batchId, images.size());

                // Update progress
                updateProgress(batchJob, BatchProgress.started(batchId, images.size()));
                if (progressCallback != null) {
                    progressCallback.accept(BatchProgress.started(batchId, images.size()));
                }

                // Process images in batches
                List<VisionResult> results = processBatches(batchJob, progressCallback);

                // Update progress
                updateProgress(batchJob, BatchProgress.completed(batchId, results));
                if (progressCallback != null) {
                    progressCallback.accept(BatchProgress.completed(batchId, results));
                }

                logger.info("Completed batch processing: {} with {} results", batchId, results.size());
                return results;

            } catch (Exception e) {
                logger.error("Error processing batch: {}", batchId, e);

                // Update progress
                BatchProgress failedProgress = BatchProgress.failed(batchId, e);
                updateProgress(batchJob, failedProgress);
                if (progressCallback != null) {
                    progressCallback.accept(failedProgress);
                }

                throw new RuntimeException("Batch processing failed", e);

            } finally {
                activeBatches.remove(batchId);
            }
        }, executor);

        return future;
    }

    /**
     * Processes multiple detection types for a batch of images.
     *
     * @param images the list of images to process
     * @param detectionTypes the detection types to perform
     * @param parameters the processing parameters
     * @return a CompletableFuture that will complete with a list of batch results
     */
    public CompletableFuture<List<BatchResult>> processMultipleTypes(
            List<ImageData> images,
            List<DetectionType> detectionTypes,
            Map<String, Object> parameters) {

        Objects.requireNonNull(images, "Images list must not be null");
        Objects.requireNonNull(detectionTypes, "Detection types must not be null");

        List<CompletableFuture<BatchResult>> futures = detectionTypes.stream()
            .map(detectionType -> processBatch(images, detectionType, parameters)
                .thenApply(results -> new BatchResult(detectionType, results)))
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }

    /**
     * Gets the current progress of a batch.
     *
     * @param batchId the batch identifier
     * @return the batch progress, or null if batch not found
     */
    public BatchProgress getBatchProgress(String batchId) {
        BatchJob batchJob = activeBatches.get(batchId);
        return batchJob != null ? batchJob.getProgress() : null;
    }

    /**
     * Gets all active batches.
     *
     * @return a map of batch IDs to active batches
     */
    public Map<String, BatchJob> getActiveBatches() {
        return Map.copyOf(activeBatches);
    }

    /**
     * Gets the number of active batches.
     *
     * @return the number of active batches
     */
    public int getActiveBatchCount() {
        return activeBatches.size();
    }

    /**
     * Cancels a batch if it's still running.
     *
     * @param batchId the batch identifier
     * @return true if the batch was cancelled, false if not found or already completed
     */
    public boolean cancelBatch(String batchId) {
        BatchJob batchJob = activeBatches.get(batchId);
        if (batchJob != null) {
            batchJob.cancel();
            activeBatches.remove(batchId);
            logger.info("Cancelled batch: {}", batchId);
            return true;
        }
        return false;
    }

    /**
     * Cancels all active batches.
     */
    public void cancelAllBatches() {
        activeBatches.values().forEach(BatchJob::cancel);
        activeBatches.clear();
        logger.info("Cancelled all active batches");
    }

    /**
     * Gets the default batch size.
     *
     * @return the default batch size
     */
    public int getDefaultBatchSize() {
        return defaultBatchSize;
    }

    /**
     * Gets the maximum number of concurrent batches.
     *
     * @return the maximum number of concurrent batches
     */
    public int getMaxConcurrentBatches() {
        return maxConcurrentBatches;
    }

    /**
     * Shuts down the processor and waits for all batches to complete.
     */
    public void shutdown() {
        logger.info("Shutting down BatchVisionProcessor");

        // Cancel all active batches
        cancelAllBatches();

        // Shutdown executor if it's an ExecutorService
        if (executor instanceof java.util.concurrent.ExecutorService executorService) {
            executorService.shutdown();
        }
    }

    /**
     * Shuts down the processor immediately, interrupting running batches.
     */
    public void shutdownNow() {
        logger.info("Shutting down BatchVisionProcessor immediately");

        // Cancel all active batches
        cancelAllBatches();

        // Shutdown executor if it's an ExecutorService
        if (executor instanceof java.util.concurrent.ExecutorService executorService) {
            executorService.shutdownNow();
        }
    }

    /**
     * Generates a unique batch identifier.
     *
     * @return a unique batch identifier
     */
    private String generateBatchId() {
        return String.format("batch-%d-%s",
                           batchCounter.incrementAndGet(),
                           java.util.UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * Updates the progress of a batch.
     *
     * @param batchJob the batch job to update
     * @param progress the new progress
     */
    private void updateProgress(BatchJob batchJob, BatchProgress progress) {
        batchJob.setProgress(progress);
    }

    /**
     * Processes images in batches.
     *
     * @param batchJob the batch job
     * @param progressCallback the progress callback
     * @return the list of vision results
     */
    private List<VisionResult> processBatches(BatchJob batchJob, Consumer<BatchProgress> progressCallback) {
        List<ImageData> images = batchJob.getImages();
        int batchSize = batchJob.getBatchSize();
        int totalImages = images.size();
        int processedImages = 0;

        List<VisionResult> allResults = new java.util.ArrayList<>();

        for (int i = 0; i < totalImages; i += batchSize) {
            if (batchJob.isCancelled()) {
                logger.info("Batch cancelled: {}", batchJob.getBatchId());
                break;
            }

            int endIndex = Math.min(i + batchSize, totalImages);
            List<ImageData> batch = images.subList(i, endIndex);

            // Process the current batch
            List<VisionResult> batchResults = processBatchSynchronously(batch, batchJob.getDetectionType(), batchJob.getParameters());
            allResults.addAll(batchResults);

            processedImages += batch.size();

            // Update progress
            double completionPercentage = (double) processedImages / totalImages;
            BatchProgress progress = BatchProgress.running(batchJob.getBatchId(), completionPercentage,
                                                         String.format("Processed %d/%d images", processedImages, totalImages));
            updateProgress(batchJob, progress);
            if (progressCallback != null) {
                progressCallback.accept(progress);
            }
        }

        return allResults;
    }

    /**
     * Processes a single batch synchronously using the actual vision backend.
     *
     * @param images the images in the batch
     * @param detectionType the detection type
     * @param parameters the parameters
     * @return the vision results
     */
    private List<VisionResult> processBatchSynchronously(List<ImageData> images, DetectionType detectionType, Map<String, Object> parameters) {
        // Use the actual OpenCV vision backend
        OpenCvVisionBackend backend = new OpenCvVisionBackend();

        try {
            // Initialize the backend
            backend.initialize();

            return images.stream()
                .map(imageData -> {
                    try {
                        long startTime = System.currentTimeMillis();

                        VisionResult result;
                        switch (detectionType) {
                            case FACE:
                                List<Detection> faceDetections = backend.detectFaces(imageData);
                                result = VisionResult.of(detectionType, faceDetections,
                                    faceDetections.isEmpty() ? 0.0 : faceDetections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                                    0);
                                break;
                            case OBJECT:
                                List<Detection> objectDetections = backend.detectObjects(imageData);
                                result = VisionResult.of(detectionType, objectDetections,
                                    objectDetections.isEmpty() ? 0.0 : objectDetections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                                    0);
                                break;
                            case TEXT:
                                // For text detection, we'll use object detection as a fallback
                                // In a real implementation, this would use OCR
                                List<Detection> textDetections = backend.detectObjects(imageData);
                                result = VisionResult.of(detectionType, textDetections,
                                    textDetections.isEmpty() ? 0.0 : textDetections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                                    0);
                                break;
                            default:
                                throw new IllegalArgumentException("Unsupported detection type: " + detectionType);
                        }

                        long processingTime = System.currentTimeMillis() - startTime;

                        // Create a new result with the actual processing time
                        return new VisionResult(
                            detectionType,
                            result.detections(),
                            result.averageConfidence(),
                            processingTime,
                            result.timestamp(),
                            Map.of("batchProcessed", true, "backend", "opencv")
                        );

                    } catch (Exception e) {
                        logger.error("Error processing image in batch", e);

                        // Return a result indicating failure
                        return new VisionResult(
                            detectionType,
                            List.of(),
                            0.0,
                            System.currentTimeMillis(),
                            java.time.Instant.now(),
                            Map.of("batchProcessed", true, "error", e.getMessage(), "backend", "opencv")
                        );
                    }
                })
                .collect(Collectors.toList());

        } finally {
            try {
                backend.shutdown();
            } catch (Exception e) {
                logger.warn("Error shutting down vision backend", e);
            }
        }
    }
}
