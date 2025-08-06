package com.springvision.examples.batchprocessing;

import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.DetectionType;
import com.springvision.core.batch.BatchVisionProcessor;
import com.springvision.core.batch.BatchProgress;
import com.springvision.core.batch.BatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Example demonstrating batch processing capabilities of Spring Vision.
 *
 * <p>This example shows how to use the BatchVisionProcessor to efficiently
 * process large volumes of images with progress monitoring, multiple detection
 * types, and concurrent processing.</p>
 *
 * <p>The example covers:</p>
 * <ul>
 *   <li>Basic batch processing</li>
 *   <li>Progress monitoring</li>
 *   <li>Multiple detection types</li>
 *   <li>Batch cancellation</li>
 *   <li>Error handling</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@SpringBootApplication
public class BatchProcessingExample {

    private static final Logger logger = LoggerFactory.getLogger(BatchProcessingExample.class);

    public static void main(String[] args) {
        SpringApplication.run(BatchProcessingExample.class, args);
    }

    /**
     * Command line runner that executes the batch processing examples.
     */
    @Component
    public static class BatchProcessingRunner implements CommandLineRunner {

        @Override
        public void run(String... args) throws Exception {
            logger.info("Starting Batch Processing Example");

            BatchVisionProcessor processor = new BatchVisionProcessor();

            try {
                // Run examples
                runBasicBatchProcessing(processor);
                runProgressMonitoringExample(processor);
                runMultipleDetectionTypesExample(processor);
                runBatchCancellationExample(processor);
                runErrorHandlingExample(processor);

            } finally {
                processor.shutdown();
                logger.info("Batch Processing Example completed");
            }
        }

        /**
         * Demonstrates basic batch processing functionality.
         */
        private void runBasicBatchProcessing(BatchVisionProcessor processor) throws Exception {
            logger.info("=== Basic Batch Processing Example ===");

            // Create test images
            List<ImageData> images = createTestImages(10);

            // Process batch
            CompletableFuture<List<VisionResult>> future = processor.processBatch(
                images,
                DetectionType.FACE,
                Map.of("confidence", 0.8)
            );

            List<VisionResult> results = future.get(30, TimeUnit.SECONDS);

            logger.info("Processed {} images with {} results", images.size(), results.size());

            // Print summary
            long successfulDetections = results.stream()
                .filter(result -> result.hasDetections())
                .count();

            double avgConfidence = results.stream()
                .mapToDouble(VisionResult::averageConfidence)
                .average()
                .orElse(0.0);

            logger.info("Successful detections: {}, Average confidence: {:.2f}",
                       successfulDetections, avgConfidence);
        }

        /**
         * Demonstrates progress monitoring functionality.
         */
        private void runProgressMonitoringExample(BatchVisionProcessor processor) throws Exception {
            logger.info("=== Progress Monitoring Example ===");

            List<ImageData> images = createTestImages(20);

            CompletableFuture<List<VisionResult>> future = processor.processBatch(
                images,
                DetectionType.OBJECT,
                Map.of("confidence", 0.7),
                progress -> {
                    logger.info("Progress: {:.1f}% - {} (Status: {})",
                               progress.getCompletionPercentage() * 100,
                               progress.getMessage(),
                               progress.getStatus());
                }
            );

            List<VisionResult> results = future.get(60, TimeUnit.SECONDS);
            logger.info("Completed processing {} images", results.size());
        }

        /**
         * Demonstrates processing multiple detection types.
         */
        private void runMultipleDetectionTypesExample(BatchVisionProcessor processor) throws Exception {
            logger.info("=== Multiple Detection Types Example ===");

            List<ImageData> images = createTestImages(15);
            List<DetectionType> detectionTypes = List.of(DetectionType.FACE, DetectionType.OBJECT, DetectionType.TEXT);

            CompletableFuture<List<BatchResult>> future = processor.processMultipleTypes(
                images,
                detectionTypes,
                Map.of("confidence", 0.8)
            );

            List<BatchResult> results = future.get(90, TimeUnit.SECONDS);

            logger.info("Processed {} detection types for {} images", detectionTypes.size(), images.size());

            // Print results for each detection type
            results.forEach(result -> {
                logger.info("{}: {} detections, avg confidence: {:.2f}",
                           result.getDetectionType(),
                           result.getSuccessfulDetections(),
                           result.getAverageConfidence());
            });
        }

        /**
         * Demonstrates batch cancellation functionality.
         */
        private void runBatchCancellationExample(BatchVisionProcessor processor) throws Exception {
            logger.info("=== Batch Cancellation Example ===");

            List<ImageData> images = createTestImages(50); // Large batch

            CompletableFuture<List<VisionResult>> future = processor.processBatch(
                images,
                DetectionType.FACE,
                Map.of("confidence", 0.8),
                progress -> {
                    // Cancel after 25% progress
                    if (progress.getCompletionPercentage() > 0.25) {
                        String batchId = progress.getBatchId();
                        if (processor.cancelBatch(batchId)) {
                            logger.info("Cancelled batch: {}", batchId);
                        }
                    }
                }
            );

            try {
                List<VisionResult> results = future.get(30, TimeUnit.SECONDS);
                logger.info("Batch completed with {} results", results.size());
            } catch (Exception e) {
                logger.info("Batch was cancelled or failed: {}", e.getMessage());
            }
        }

        /**
         * Demonstrates error handling in batch processing.
         */
        private void runErrorHandlingExample(BatchVisionProcessor processor) throws Exception {
            logger.info("=== Error Handling Example ===");

            // This example shows how to handle errors gracefully
            try {
                CompletableFuture<List<VisionResult>> future = processor.processBatch(
                    List.of(), // Empty list should be handled gracefully
                    DetectionType.FACE,
                    Map.of("confidence", 0.8)
                );

                List<VisionResult> results = future.get(10, TimeUnit.SECONDS);
                logger.info("Empty batch processed successfully: {} results", results.size());

            } catch (Exception e) {
                logger.error("Error in batch processing: {}", e.getMessage());
            }
        }

        /**
         * Creates a list of test images for demonstration purposes.
         *
         * @param count the number of test images to create
         * @return list of test images
         */
        private List<ImageData> createTestImages(int count) {
            List<ImageData> images = new java.util.ArrayList<>();

            for (int i = 0; i < count; i++) {
                byte[] imageBytes = createTestImageBytes(100 + i * 10, 100 + i * 5);
                images.add(ImageData.fromBytes(imageBytes));
            }

            return images;
        }

        /**
         * Creates test image bytes for demonstration purposes.
         *
         * @param width the image width
         * @param height the image height
         * @return byte array representing the image
         */
        private byte[] createTestImageBytes(int width, int height) {
            try {
                // Create a simple colored rectangle as a PNG
                java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g2d = image.createGraphics();
                g2d.setColor(java.awt.Color.BLUE);
                g2d.fillRect(0, 0, width, height);
                g2d.setColor(java.awt.Color.RED);
                g2d.fillRect(width/4, height/4, width/2, height/2);
                g2d.dispose();

                // Convert to PNG bytes
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(image, "PNG", baos);
                return baos.toByteArray();
            } catch (Exception e) {
                logger.warn("Failed to create test image, using fallback", e);
                return String.format("test-image-%dx%d", width, height).getBytes();
            }
        }
    }
}
