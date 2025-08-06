/**
 * Batch processing capabilities for computer vision operations.
 *
 * <p>This package provides comprehensive batch processing functionality for computer vision
 * operations, enabling efficient processing of large volumes of images with configurable
 * batch sizes, parallel processing, and progress monitoring.</p>
 *
 * <h2>Key Components</h2>
 *
 * <h3>{@link com.springvision.core.batch.BatchVisionProcessor}</h3>
 * <p>The main batch processor that orchestrates the processing of multiple images.
 * It provides methods for:</p>
 * <ul>
 *   <li>Processing images in configurable batch sizes</li>
 *   <li>Parallel processing with progress monitoring</li>
 *   <li>Multiple detection type processing</li>
 *   <li>Batch cancellation and management</li>
 * </ul>
 *
 * <h3>{@link com.springvision.core.batch.BatchJob}</h3>
 * <p>Represents a batch processing job with its configuration and state.
 * Encapsulates all information needed to process a batch of images.</p>
 *
 * <h3>{@link com.springvision.core.batch.BatchProgress}</h3>
 * <p>Immutable progress information for batch processing operations.
 * Provides status updates, completion percentages, and result metadata.</p>
 *
 * <h3>{@link com.springvision.core.batch.BatchResult}</h3>
 * <p>Represents the result of processing a batch with a specific detection type.
 * Provides aggregated statistics and results for batch operations.</p>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Batch Processing</h3>
 * <pre>{@code
 * BatchVisionProcessor processor = new BatchVisionProcessor();
 *
 * List<ImageData> images = // ... list of images
 *
 * CompletableFuture<List<VisionResult>> future = processor.processBatch(
 *     images,
 *     DetectionType.FACE,
 *     Map.of("confidence", 0.8)
 * );
 *
 * List<VisionResult> results = future.get();
 * }</pre>
 *
 * <h3>Batch Processing with Progress Monitoring</h3>
 * <pre>{@code
 * CompletableFuture<List<VisionResult>> future = processor.processBatch(
 *     images,
 *     DetectionType.OBJECT,
 *     Map.of("confidence", 0.7),
 *     progress -> {
 *         System.out.printf("Progress: %.1f%% - %s%n",
 *                          progress.getCompletionPercentage() * 100,
 *                          progress.getMessage());
 *     }
 * );
 * }</pre>
 *
 * <h3>Multiple Detection Types</h3>
 * <pre>{@code
 * List<DetectionType> types = List.of(DetectionType.FACE, DetectionType.OBJECT);
 *
 * CompletableFuture<List<BatchResult>> future = processor.processMultipleTypes(
 *     images,
 *     types,
 *     Map.of("confidence", 0.8)
 * );
 *
 * List<BatchResult> results = future.get();
 * results.forEach(result -> {
 *     System.out.printf("%s: %d detections, avg confidence: %.2f%n",
 *                      result.getDetectionType(),
 *                      result.getSuccessfulDetections(),
 *                      result.getAverageConfidence());
 * });
 * }</pre>
 *
 * <h2>Configuration</h2>
 *
 * <p>The batch processor can be configured with:</p>
 * <ul>
 *   <li><strong>Batch Size:</strong> Number of images processed in each batch</li>
 *   <li><strong>Concurrency:</strong> Maximum number of concurrent batches</li>
 *   <li><strong>Executor:</strong> Custom thread pool for processing</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 *
 * <p>All classes in this package are thread-safe and designed for concurrent use.
 * The batch processor uses thread-safe collections and atomic operations to ensure
 * safe concurrent access.</p>
 *
 * <h2>Error Handling</h2>
 *
 * <p>The batch processor provides comprehensive error handling:</p>
 * <ul>
 *   <li>Individual batch failures don't affect other batches</li>
 *   <li>Progress callbacks include error information</li>
 *   <li>Graceful cancellation support</li>
 *   <li>Resource cleanup on shutdown</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see com.springvision.core.VisionTemplate
 * @see com.springvision.core.VisionResult
 * @see com.springvision.core.DetectionType
 */
package com.springvision.core.batch;
