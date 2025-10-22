package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Comprehensive integration test for performance and concurrency aspects of DJL Vision Backend.
 * Tests concurrent inference, performance metrics, resource management, and scalability.
 */
@DisplayName("Performance & Concurrency Integration Tests")
public class PerformanceConcurrencyIntegrationTest {

    private static DjlVisionBackend backend;
    private static ExecutorService executorService;

    @BeforeAll
    static void setup() throws Exception {
        // Configure backend for performance testing
        System.setProperty("ai.djl.offline", "true");
        System.setProperty("OPT_OUT_TRACKING", "true");

        DjlProperties properties = new DjlProperties();
        properties.setEngine("PyTorch");
        properties.setDevice("cpu");
        properties.setAutoDownload(false); // Use pre-downloaded models

        // Configure for performance testing with higher concurrency
        properties.getObjectDetection().setModel("yolo");
        properties.getObjectDetection().setConfidenceThreshold(0.3f);
        properties.getFaceDetection().setConfidenceThreshold(0.5f);

        backend = new DjlVisionBackend(properties);

        // Initialize with models
        backend.initialize();

        // Verify backend is ready
        assertThat(backend.isHealthy()).isTrue();

        // Create thread pool for concurrent testing
        int numThreads = Math.min(8, Runtime.getRuntime().availableProcessors());
        executorService = Executors.newFixedThreadPool(numThreads);
    }

    @AfterAll
    static void teardown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (backend != null) {
            backend.shutdown();
        }
    }

    // ==================== Concurrency Tests ====================

    @Test
    @DisplayName("Should handle concurrent object detection requests")
    void shouldHandleConcurrentObjectDetectionRequests() throws Exception {
        // Given: Multiple test images
        List<ImageData> testImages = createTestImages(10);

        // When: Multiple object detection requests are executed concurrently
        List<CompletableFuture<List<Detection>>> futures = testImages.stream()
            .map(image -> CompletableFuture.supplyAsync(() -> {
                try {
                    return backend.detectObjects(image);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorService))
            .toList();

        // Wait for all to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));

        // Then: All requests should complete successfully
        assertThat(allFutures.get(30, TimeUnit.SECONDS)).isNull();

        // Verify all results are valid
        for (CompletableFuture<List<Detection>> future : futures) {
            List<Detection> detections = future.get();
            assertThat(detections).isNotNull();
        }
    }

    @Test
    @DisplayName("Should handle concurrent face detection requests")
    void shouldHandleConcurrentFaceDetectionRequests() throws Exception {
        // Given: Multiple face-like images
        List<ImageData> faceImages = IntStream.range(0, 8)
            .mapToObj(i -> {
                try {
                    return TestImageUtils.createSimpleFaceImage(320, 240);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .toList();

        // When: Multiple face detection requests are executed concurrently
        List<CompletableFuture<List<Detection>>> futures = faceImages.stream()
            .map(image -> CompletableFuture.supplyAsync(() -> backend.detectFaces(image), executorService))
            .toList();

        // Wait for all to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));

        // Then: All requests should complete successfully
        assertThat(allFutures.get(30, TimeUnit.SECONDS)).isNull();

        // Verify all results are valid
        for (CompletableFuture<List<Detection>> future : futures) {
            List<Detection> detections = future.get();
            assertThat(detections).isNotNull();
        }
    }

    @Test
    @DisplayName("Should handle mixed concurrent operations")
    void shouldHandleMixedConcurrentOperations() throws Exception {
        // Given: Mix of different operations
        List<Runnable> operations = new ArrayList<>();

        // Add various operations
        for (int i = 0; i < 5; i++) {
            operations.add(() -> {
                try {
                    ImageData image = TestImageUtils.createRectangleImage(640, 480, Color.BLUE);
                    backend.detectObjects(image);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            operations.add(() -> {
                try {
                    ImageData image = TestImageUtils.createSimpleFaceImage(320, 240);
                    backend.detectFaces(image);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            operations.add(() -> {
                try {
                    ImageData image = TestImageUtils.createTextImage("TEST", 400, 100);
                    backend.extractText(image);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // When: Mixed operations are executed concurrently
        List<CompletableFuture<Void>> futures = operations.stream()
            .map(operation -> CompletableFuture.runAsync(operation, executorService))
            .toList();

        // Wait for all to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));

        // Then: All operations should complete successfully
        assertThat(allFutures.get(60, TimeUnit.SECONDS)).isNull();
    }

    // ==================== Performance Tests ====================

    @Test
    @DisplayName("Should maintain performance under load")
    void shouldMaintainPerformanceUnderLoad() throws Exception {
        // Given: Large number of images for performance testing
        List<ImageData> testImages = createTestImages(20);

        // When: Performance test is executed
        long startTime = System.nanoTime();

        List<CompletableFuture<List<Detection>>> futures = testImages.stream()
            .map(image -> CompletableFuture.supplyAsync(() -> {
                try {
                    return backend.detectObjects(image);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorService))
            .toList();

        // Wait for all to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        allFutures.get(120, TimeUnit.SECONDS); // Longer timeout for performance test

        long endTime = System.nanoTime();
        double totalTimeSeconds = (endTime - startTime) / 1_000_000_000.0;
        double avgTimePerImage = totalTimeSeconds / testImages.size();

        // Then: Performance should be reasonable (less than 5 seconds per image on average)
        assertThat(avgTimePerImage).isLessThan(5.0);
        assertThat(totalTimeSeconds).isGreaterThan(0.0);

        System.out.printf("Performance test: %d images processed in %.2f seconds (%.2f sec/image)%n",
            testImages.size(), totalTimeSeconds, avgTimePerImage);
    }

    @Test
    @DisplayName("Should handle rapid sequential requests")
    void shouldHandleRapidSequentialRequests() throws Exception {
        // Given: Series of rapid sequential requests
        ImageData testImage = TestImageUtils.createRectangleImage(640, 480, Color.RED);

        // When: Multiple sequential requests are made rapidly
        long startTime = System.nanoTime();
        int numRequests = 20;

        for (int i = 0; i < numRequests; i++) {
            List<Detection> detections = backend.detectObjects(testImage);
            assertThat(detections).isNotNull();
        }

        long endTime = System.nanoTime();
        double totalTimeSeconds = (endTime - startTime) / 1_000_000_000.0;
        double avgTimePerRequest = totalTimeSeconds / numRequests;

        // Then: Should handle rapid requests without significant degradation
        assertThat(avgTimePerRequest).isLessThan(2.0); // Less than 2 seconds per request
        assertThat(totalTimeSeconds).isGreaterThan(0.0);

        System.out.printf("Sequential test: %d requests in %.2f seconds (%.2f sec/request)%n",
            numRequests, totalTimeSeconds, avgTimePerRequest);
    }

    // ==================== Resource Management Tests ====================

    @Test
    @DisplayName("Should properly manage model resources")
    void shouldProperlyManageModelResources() throws Exception {
        // Given: Multiple inference operations
        ImageData testImage = TestImageUtils.createRectangleImage(640, 480, Color.GREEN);

        // When: Many inference operations are performed
        for (int i = 0; i < 50; i++) {
            List<Detection> detections = backend.detectObjects(testImage);
            assertThat(detections).isNotNull();

            // Occasional face detection to test multiple models
            if (i % 10 == 0) {
                List<Detection> faces = backend.detectFaces(testImage);
                assertThat(faces).isNotNull();
            }
        }

        // Then: Backend should still be healthy and responsive
        assertThat(backend.isHealthy()).isTrue();

        // Final test to ensure backend is still functional
        List<Detection> finalDetections = backend.detectObjects(testImage);
        assertThat(finalDetections).isNotNull();
    }

    @Test
    @DisplayName("Should handle memory-intensive operations")
    void shouldHandleMemoryIntensiveOperations() throws Exception {
        // Given: Large images for memory testing
        List<ImageData> largeImages = List.of(
            TestImageUtils.createRectangleImage(1920, 1080, Color.BLUE), // Full HD
            TestImageUtils.createRectangleImage(1200, 800, Color.RED),
            TestImageUtils.createRectangleImage(800, 600, Color.GREEN)
        );

        // When: Large image processing is performed
        for (ImageData largeImage : largeImages) {
            List<Detection> detections = backend.detectObjects(largeImage);
            assertThat(detections).isNotNull();

            // Force garbage collection between operations
            System.gc();
            Thread.sleep(100);
        }

        // Then: Backend should remain stable
        assertThat(backend.isHealthy()).isTrue();
    }

    // ==================== Scalability Tests ====================

    @Test
    @DisplayName("Should scale with different concurrency levels")
    void shouldScaleWithDifferentConcurrencyLevels() throws Exception {
        // Given: Test images for scalability testing
        List<ImageData> testImages = createTestImages(12);

        // Test different concurrency levels
        int[] concurrencyLevels = {1, 2, 4};

        for (int concurrency : concurrencyLevels) {
            ExecutorService testExecutor = Executors.newFixedThreadPool(concurrency);

            try {
                // When: Operations are performed with specific concurrency
                long startTime = System.nanoTime();

                List<CompletableFuture<List<Detection>>> futures = testImages.stream()
                    .map(image -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return backend.detectObjects(image);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }, testExecutor))
                    .toList();

                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
                allFutures.get(60, TimeUnit.SECONDS);

                long endTime = System.nanoTime();
                double totalTimeSeconds = (endTime - startTime) / 1_000_000_000.0;

                // Then: Operations should complete successfully
                assertThat(totalTimeSeconds).isGreaterThan(0.0);
                assertThat(totalTimeSeconds).isLessThan(120.0); // Reasonable timeout

                System.out.printf("Concurrency test (level %d): %.2f seconds for %d images%n",
                    concurrency, totalTimeSeconds, testImages.size());

            } finally {
                testExecutor.shutdown();
                testExecutor.awaitTermination(10, TimeUnit.SECONDS);
            }
        }
    }

    // ==================== Error Handling Under Load ====================

    @Test
    @DisplayName("Should handle errors gracefully under load")
    void shouldHandleErrorsGracefullyUnderLoad() throws Exception {
        // Given: Mix of valid and invalid operations
        List<CompletableFuture<List<Detection>>> futures = new ArrayList<>();

        // Add valid operations
        for (int i = 0; i < 5; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    ImageData image = TestImageUtils.createRectangleImage(640, 480, Color.BLUE);
                    return backend.detectObjects(image);
                } catch (Exception e) {
                    return new ArrayList<Detection>(); // Return empty on error
                }
            }, executorService));
        }

        // Add operations that might fail
        for (int i = 0; i < 3; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    // Try pose estimation which might not be available
                    ImageData image = TestImageUtils.createPersonSilhouetteImage(640, 480);
                    return backend.detectPoses(image);
                } catch (Exception e) {
                    return new ArrayList<Detection>(); // Return empty on error
                }
            }, executorService));
        }

        // When: All operations are executed
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));

        // Then: Should complete without crashing (some may fail gracefully)
        assertThat(allFutures.get(30, TimeUnit.SECONDS)).isNull();

        // Verify results are obtainable
        for (CompletableFuture<List<Detection>> future : futures) {
            List<Detection> detections = future.get();
            assertThat(detections).isNotNull(); // Should not be null even on error
        }
    }

    // ==================== Helper Methods ====================

    private List<ImageData> createTestImages(int count) {
        List<ImageData> images = new ArrayList<>();
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE};

        for (int i = 0; i < count; i++) {
            try {
                Color color = colors[i % colors.length];
                images.add(TestImageUtils.createRectangleImage(640, 480, color));
            } catch (IOException e) {
                throw new RuntimeException("Failed to create test image", e);
            }
        }

        return images;
    }
}
