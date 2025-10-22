package io.github.codesapienbe.springvision.core.djl;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Performance and stress tests for DJL backend.
 */
@Disabled("Performance tests - enable manually for benchmarking")
class DjlPerformanceTest {

    private DjlVisionBackend backend;
    private DjlProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DjlProperties();
        properties.setEnabled(true);
        properties.setEngine("PyTorch");
        properties.setDevice("cpu");
        properties.setMaxConcurrentInferences(4);

        backend = new DjlVisionBackend(properties);
    }

    @Test
    void testMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();

        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Create backend and process images
        byte[] imageData = createTestImage(640, 480);

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        // Memory usage should be reasonable (adjust threshold as needed)
        assertTrue(memoryUsed < 500_000_000,
            "Memory usage too high: " + (memoryUsed / 1_000_000) + " MB");
    }

    @Test
    void testConcurrentProcessing() throws Exception {
        backend.initialize();

        int numberOfThreads = 10;
        int imagesPerThread = 5;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<Boolean>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            Future<Boolean> future = executor.submit(() -> {
                try {
                    for (int j = 0; j < imagesPerThread; j++) {
                        byte[] imageData = createTestImage(320, 240);
                        ImageData image = new ImageData(
                            imageData,
                            "image/png",
                            System.currentTimeMillis(),
                            "test-" + threadId + "-" + j
                        );
                        backend.detectFaces(image);
                    }
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            });
            futures.add(future);
        }

        // Wait for all tasks to complete
        for (Future<Boolean> future : futures) {
            assertTrue(future.get(60, TimeUnit.SECONDS));
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();

        // Log performance metrics
        System.out.println("Processed " + (numberOfThreads * imagesPerThread) +
            " images in " + duration + "ms");
        System.out.println("Average: " + (duration / (numberOfThreads * imagesPerThread)) +
            "ms per image");

        // Should complete in reasonable time
        assertTrue(duration < 60000, "Processing took too long: " + duration + "ms");
    }

    @Test
    void testLargeImageProcessing() throws Exception {
        backend.initialize();

        // Test with various image sizes
        int[][] sizes = {
            {640, 480},
            {1280, 720},
            {1920, 1080},
            {3840, 2160}  // 4K
        };

        for (int[] size : sizes) {
            byte[] imageData = createTestImage(size[0], size[1]);
            ImageData image = new ImageData(
                imageData,
                "image/png",
                System.currentTimeMillis(),
                "large-" + size[0] + "x" + size[1]
            );

            long startTime = System.nanoTime();
            backend.detectFaces(image);
            long endTime = System.nanoTime();

            long durationMs = (endTime - startTime) / 1_000_000;

            System.out.println("Size " + size[0] + "x" + size[1] +
                " processed in " + durationMs + "ms");

            // Should complete in reasonable time even for large images
            assertTrue(durationMs < 10000,
                "Large image processing took too long: " + durationMs + "ms");
        }
    }

    @Test
    void testRepeatedInitializationAndShutdown() {
        for (int i = 0; i < 5; i++) {
            try {
                backend.initialize();
                assertTrue(backend.isHealthy());
                backend.shutdown();
                assertFalse(backend.isHealthy());
            } catch (Exception e) {
                fail("Initialization/shutdown cycle " + i + " failed: " + e.getMessage());
            }
        }
    }

    @Test
    void testBatchProcessing() throws Exception {
        backend.initialize();

        int batchSize = 100;
        List<ImageData> images = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            byte[] imageData = createTestImage(320, 240);
            images.add(new ImageData(
                imageData,
                "image/png",
                System.currentTimeMillis(),
                "batch-" + i
            ));
        }

        long startTime = System.currentTimeMillis();

        for (ImageData image : images) {
            backend.detectFaces(image);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Batch processed " + batchSize + " images in " +
            duration + "ms");
        System.out.println("Average: " + (duration / batchSize) + "ms per image");

        assertTrue(duration < 30000, "Batch processing took too long: " + duration + "ms");
    }

    @Test
    void testResourceCleanup() throws Exception {
        backend.initialize();

        // Process some images
        for (int i = 0; i < 10; i++) {
            byte[] imageData = createTestImage(320, 240);
            ImageData image = new ImageData(
                imageData,
                "image/png",
                System.currentTimeMillis(),
                "cleanup-test-" + i
            );
            backend.detectFaces(image);
        }

        // Force garbage collection
        System.gc();
        Thread.sleep(1000);

        // Shutdown and verify cleanup
        backend.shutdown();
        assertFalse(backend.isHealthy());

        // Memory should be released (basic check)
        System.gc();
        Thread.sleep(1000);

        long freeMemory = Runtime.getRuntime().freeMemory();
        assertTrue(freeMemory > 0, "No free memory after cleanup");
    }

    private byte[] createTestImage(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.BLUE);
            g.fillRect(width / 4, height / 4, width / 2, height / 2);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
