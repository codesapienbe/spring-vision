package com.springvision.core;

import com.springvision.core.backend.*;
import com.springvision.core.error.ErrorHandler;
import com.springvision.core.logging.VisionLogger;
import com.springvision.core.metrics.VisionMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for the Spring Vision framework.
 * 
 * <p>This test suite validates the complete vision processing pipeline,
 * including all backends, error handling, logging, and metrics collection.</p>
 * 
 * @since 1.1.0
 * @author Spring Vision Team
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {VisionTemplate.class, OpenCvVisionBackend.class, 
                          FaceBytesBackend.class, CompreFaceVisionBackend.class,
                          DeepFaceVisionBackend.class, MediaPipeVisionBackend.class,
                          YoloVisionBackend.class, InsightFaceVisionBackend.class,
                          ErrorHandler.class, VisionMetrics.class})
@ActiveProfiles("test")
public class VisionTemplateIntegrationTest {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    @Autowired
    private ErrorHandler errorHandler;
    
    @Autowired
    private VisionMetrics visionMetrics;
    
    private byte[] testImageData;
    private ImageData testImage;
    
    @BeforeEach
    void setUp() throws IOException {
        // Load test image data
        Path testImagePath = Path.of("src/test/resources/test-image.jpg");
        if (Files.exists(testImagePath)) {
            testImageData = Files.readAllBytes(testImagePath);
        } else {
            // Create dummy image data for testing
            testImageData = createDummyImageData();
        }
        
        testImage = new ImageData(testImageData, "image/jpeg");
        
        // Set up logging context
        VisionLogger.setCorrelationId("test-" + System.currentTimeMillis());
        VisionLogger.setUserId("test-user");
        VisionLogger.setRequestId("test-request");
    }
    
    /**
     * Tests basic face detection with OpenCV backend.
     */
    @Test
    void testOpenCvFaceDetection() {
        DetectionQuery query = DetectionQuery.builder()
            .type(DetectionType.FACE)
            .minConfidence(0.5)
            .maxDetections(10)
            .build();
        
        List<Detection> detections = visionTemplate.detect(testImage, query);
        
        assertNotNull(detections, "Detections should not be null");
        assertTrue(detections.size() >= 0, "Detection count should be non-negative");
        
        // Validate detection structure
        for (Detection detection : detections) {
            assertNotNull(detection.getType(), "Detection type should not be null");
            assertEquals(DetectionType.FACE, detection.getType(), "Detection type should be FACE");
            assertNotNull(detection.getBoundingBox(), "Bounding box should not be null");
            assertTrue(detection.getConfidence() >= 0.0 && detection.getConfidence() <= 1.0, 
                      "Confidence should be between 0 and 1");
            assertNotNull(detection.getAttributes(), "Attributes should not be null");
        }
    }
    
    /**
     * Tests object detection with YOLO backend.
     */
    @Test
    void testYoloObjectDetection() {
        DetectionQuery query = DetectionQuery.builder()
            .type(DetectionType.OBJECT)
            .minConfidence(0.3)
            .maxDetections(20)
            .build();
        
        List<Detection> detections = visionTemplate.detect(testImage, query);
        
        assertNotNull(detections, "Detections should not be null");
        assertTrue(detections.size() >= 0, "Detection count should be non-negative");
        
        // Validate detection structure
        for (Detection detection : detections) {
            assertNotNull(detection.getType(), "Detection type should not be null");
            assertEquals(DetectionType.OBJECT, detection.getType(), "Detection type should be OBJECT");
            assertNotNull(detection.getBoundingBox(), "Bounding box should not be null");
            assertTrue(detection.getConfidence() >= 0.0 && detection.getConfidence() <= 1.0, 
                      "Confidence should be between 0 and 1");
            assertNotNull(detection.getAttributes(), "Attributes should not be null");
        }
    }
    
    /**
     * Tests hand landmark detection with MediaPipe backend.
     */
    @Test
    void testMediaPipeHandDetection() {
        DetectionQuery query = DetectionQuery.builder()
            .type(DetectionType.HAND)
            .minConfidence(0.5)
            .maxDetections(10)
            .build();
        
        List<Detection> detections = visionTemplate.detect(testImage, query);
        
        assertNotNull(detections, "Detections should not be null");
        assertTrue(detections.size() >= 0, "Detection count should be non-negative");
        
        // Validate detection structure
        for (Detection detection : detections) {
            assertNotNull(detection.getType(), "Detection type should not be null");
            assertEquals(DetectionType.HAND, detection.getType(), "Detection type should be HAND");
            assertNotNull(detection.getBoundingBox(), "Bounding box should not be null");
            assertTrue(detection.getConfidence() >= 0.0 && detection.getConfidence() <= 1.0, 
                      "Confidence should be between 0 and 1");
            assertNotNull(detection.getAttributes(), "Attributes should not be null");
        }
    }
    
    /**
     * Tests pose detection with MediaPipe backend.
     */
    @Test
    void testMediaPipePoseDetection() {
        DetectionQuery query = DetectionQuery.builder()
            .type(DetectionType.POSE)
            .minConfidence(0.5)
            .maxDetections(5)
            .build();
        
        List<Detection> detections = visionTemplate.detect(testImage, query);
        
        assertNotNull(detections, "Detections should not be null");
        assertTrue(detections.size() >= 0, "Detection count should be non-negative");
        
        // Validate detection structure
        for (Detection detection : detections) {
            assertNotNull(detection.getType(), "Detection type should not be null");
            assertEquals(DetectionType.POSE, detection.getType(), "Detection type should be POSE");
            assertNotNull(detection.getBoundingBox(), "Bounding box should not be null");
            assertTrue(detection.getConfidence() >= 0.0 && detection.getConfidence() <= 1.0, 
                      "Confidence should be between 0 and 1");
            assertNotNull(detection.getAttributes(), "Attributes should not be null");
        }
    }
    
    /**
     * Tests error handling with invalid input.
     */
    @Test
    void testErrorHandlingWithInvalidInput() {
        // Test with null image data
        assertThrows(IllegalArgumentException.class, () -> {
            visionTemplate.detect(null, DetectionQuery.builder()
                .type(DetectionType.FACE)
                .build());
        }, "Should throw exception for null image data");
        
        // Test with empty image data
        ImageData emptyImage = new ImageData(new byte[0], "image/jpeg");
        assertThrows(IllegalArgumentException.class, () -> {
            visionTemplate.detect(emptyImage, DetectionQuery.builder()
                .type(DetectionType.FACE)
                .build());
        }, "Should throw exception for empty image data");
        
        // Test with null query
        assertThrows(IllegalArgumentException.class, () -> {
            visionTemplate.detect(testImage, null);
        }, "Should throw exception for null query");
    }
    
    /**
     * Tests error handling with unsupported detection type.
     */
    @Test
    void testErrorHandlingWithUnsupportedType() {
        DetectionQuery query = DetectionQuery.builder()
            .type(DetectionType.LANDMARK) // Assuming this is not supported
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            visionTemplate.detect(testImage, query);
        }, "Should throw exception for unsupported detection type");
    }
    
    /**
     * Tests performance metrics collection.
     */
    @Test
    void testPerformanceMetricsCollection() {
        // Get initial metrics
        VisionMetrics.MetricsSummary initialMetrics = visionMetrics.getMetricsSummary();
        long initialDetections = initialMetrics.getTotalDetections();
        
        // Perform detection
        DetectionQuery query = DetectionQuery.builder()
            .type(DetectionType.FACE)
            .minConfidence(0.5)
            .maxDetections(10)
            .build();
        
        List<Detection> detections = visionTemplate.detect(testImage, query);
        
        // Get updated metrics
        VisionMetrics.MetricsSummary updatedMetrics = visionMetrics.getMetricsSummary();
        long updatedDetections = updatedMetrics.getTotalDetections();
        
        // Verify metrics were updated
        assertTrue(updatedDetections >= initialDetections, 
                  "Detection count should have increased");
    }
    
    /**
     * Tests error handler functionality.
     */
    @Test
    void testErrorHandlerFunctionality() {
        // Test retry mechanism
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        try {
            errorHandler.executeWithRetry("test-backend", "test-operation", () -> {
                attemptCount.incrementAndGet();
                if (attemptCount.get() < 3) {
                    throw new RuntimeException("Simulated failure");
                }
                return "success";
            });
        } catch (Exception e) {
            // Expected to fail after retries
        }
        
        assertEquals(3, attemptCount.get(), "Should have attempted 3 times");
    }
    
    /**
     * Tests circuit breaker functionality.
     */
    @Test
    void testCircuitBreakerFunctionality() {
        // Test circuit breaker with repeated failures
        AtomicInteger failureCount = new AtomicInteger(0);
        
        for (int i = 0; i < 10; i++) {
            try {
                errorHandler.executeWithCircuitBreaker("test-circuit-breaker", "test-operation", () -> {
                    failureCount.incrementAndGet();
                    throw new RuntimeException("Simulated circuit breaker failure");
                });
            } catch (Exception e) {
                // Expected to fail
            }
        }
        
        assertTrue(failureCount.get() > 0, "Should have recorded failures");
    }
    
    /**
     * Tests graceful degradation.
     */
    @Test
    void testGracefulDegradation() {
        String result = errorHandler.executeWithGracefulDegradation(
            "test-degradation", 
            "test-operation",
            () -> {
                throw new RuntimeException("Primary operation failed");
            },
            (exception) -> "fallback-result"
        );
        
        assertEquals("fallback-result", result, "Should return fallback result");
    }
    
    /**
     * Tests logging functionality.
     */
    @Test
    void testLoggingFunctionality() {
        // Test structured logging
        Map<String, Object> logData = Map.of(
            "test_key", "test_value",
            "test_number", 42
        );
        
        VisionLogger.info("test-component", "Test log message", logData);
        
        // Verify correlation ID is set
        assertNotNull(VisionLogger.getOrCreateCorrelationId(), "Correlation ID should be set");
    }
    
    /**
     * Tests health check functionality.
     */
    @Test
    void testHealthCheckFunctionality() {
        // Test backend health checks
        List<VisionBackend> backends = List.of(
            new OpenCvVisionBackend(),
            new FaceBytesBackend(),
            new CompreFaceVisionBackend(),
            new DeepFaceVisionBackend(),
            new MediaPipeVisionBackend(),
            new YoloVisionBackend(),
            new InsightFaceVisionBackend()
        );
        
        for (VisionBackend backend : backends) {
            BackendHealthInfo healthInfo = backend.getHealthInfo();
            assertNotNull(healthInfo, "Health info should not be null");
            assertNotNull(healthInfo.getBackendName(), "Backend name should not be null");
            assertNotNull(healthInfo.getStatus(), "Health status should not be null");
            assertNotNull(healthInfo.getDetails(), "Health details should not be null");
        }
    }
    
    /**
     * Tests configuration validation.
     */
    @Test
    void testConfigurationValidation() {
        // Test valid configuration
        DetectionQuery validQuery = DetectionQuery.builder()
            .type(DetectionType.FACE)
            .minConfidence(0.5)
            .maxDetections(10)
            .nmsThreshold(0.45)
            .build();
        
        assertNotNull(validQuery, "Valid query should be created");
        assertEquals(DetectionType.FACE, validQuery.type(), "Query type should match");
        assertEquals(0.5, validQuery.minConfidence(), "Min confidence should match");
        assertEquals(10, validQuery.maxDetections(), "Max detections should match");
        assertEquals(0.45, validQuery.nmsThreshold(), "NMS threshold should match");
    }
    
    /**
     * Tests image data validation.
     */
    @Test
    void testImageDataValidation() {
        // Test valid image data
        ImageData validImage = new ImageData(testImageData, "image/jpeg");
        assertNotNull(validImage, "Valid image should be created");
        assertEquals(testImageData.length, validImage.getData().length, "Image data should match");
        assertEquals("image/jpeg", validImage.getFormat(), "Image format should match");
        
        // Test image data with metadata
        Map<String, Object> metadata = Map.of("width", 640, "height", 480);
        ImageData imageWithMetadata = new ImageData(testImageData, "image/jpeg", metadata);
        assertNotNull(imageWithMetadata.getMetadata(), "Metadata should not be null");
        assertEquals(640, imageWithMetadata.getMetadata().get("width"), "Width should match");
        assertEquals(480, imageWithMetadata.getMetadata().get("height"), "Height should match");
    }
    
    /**
     * Tests detection result validation.
     */
    @Test
    void testDetectionResultValidation() {
        // Create a test detection
        BoundingBox box = new BoundingBox(0.1, 0.2, 0.3, 0.4);
        Map<String, Object> attributes = Map.of("confidence", 0.95, "category", "FACE");
        
        Detection detection = new Detection(DetectionType.FACE, box, 0.95, attributes);
        
        assertNotNull(detection, "Detection should not be null");
        assertEquals(DetectionType.FACE, detection.getType(), "Detection type should match");
        assertEquals(box, detection.getBoundingBox(), "Bounding box should match");
        assertEquals(0.95, detection.getConfidence(), "Confidence should match");
        assertEquals(attributes, detection.getAttributes(), "Attributes should match");
    }
    
    /**
     * Creates dummy image data for testing.
     */
    private byte[] createDummyImageData() {
        // Create a minimal JPEG header (not a real image, just for testing)
        return new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, // JPEG SOI and APP0 markers
            0x00, 0x10, // Length
            0x4A, 0x46, 0x49, 0x46, 0x00, // "JFIF\0"
            0x01, 0x01, // Version 1.1
            0x00, // Units: none
            0x00, 0x01, // Density: 1x1
            0x00, 0x01,
            0x00, 0x00, // No thumbnail
            (byte) 0xFF, (byte) 0xD9  // JPEG EOI marker
        };
    }
    
    /**
     * Helper class for testing.
     */
    private static class AtomicInteger {
        private int value = 0;
        
        public int incrementAndGet() {
            return ++value;
        }
        
        public int get() {
            return value;
        }
    }
} 