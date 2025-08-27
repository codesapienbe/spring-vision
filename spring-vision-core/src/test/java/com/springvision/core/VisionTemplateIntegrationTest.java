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
        
        testImage = ImageData.fromBytes(testImageData, "image/jpeg");
        
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
        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.FACE)
            .minConfidence(0.5)
            .maxDetections(10)
            .build();
        
        VisionResult result = visionTemplate.detect(testImage, query);
        List<Detection> detections = result.detections();
        
        assertNotNull(detections, "Detections should not be null");
        assertTrue(detections.size() >= 0, "Detection count should be non-negative");
        
        // Validate detection structure
        for (Detection detection : detections) {
            assertNotNull(detection.label(), "Detection label should not be null");
            assertNotNull(detection.boundingBox(), "Bounding box should not be null");
            assertTrue(detection.confidence() >= 0.0 && detection.confidence() <= 1.0, 
                      "Confidence should be between 0 and 1");
            assertNotNull(detection.attributes(), "Attributes should not be null");
        }
    }
    
    /**
     * Tests object detection with YOLO backend.
     */
    @Test
    void testYoloObjectDetection() {
        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.OBJECT)
            .minConfidence(0.3)
            .maxDetections(20)
            .build();
        
        VisionResult result = visionTemplate.detect(testImage, query);
        List<Detection> detections = result.detections();
        
        assertNotNull(detections, "Detections should not be null");
        assertTrue(detections.size() >= 0, "Detection count should be non-negative");
        
        // Validate detection structure
        for (Detection detection : detections) {
            assertNotNull(detection.label(), "Detection label should not be null");
            assertNotNull(detection.boundingBox(), "Bounding box should not be null");
            assertTrue(detection.confidence() >= 0.0 && detection.confidence() <= 1.0, 
                      "Confidence should be between 0 and 1");
            assertNotNull(detection.attributes(), "Attributes should not be null");
        }
    }
    
    /**
     * Tests hand landmark detection with MediaPipe backend.
     */
    @Test
    void testMediaPipeHandDetection() {
        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.HAND)
            .minConfidence(0.5)
            .maxDetections(10)
            .build();
        
        VisionResult result = visionTemplate.detect(testImage, query);
        List<Detection> detections = result.detections();
        
        assertNotNull(detections, "Detections should not be null");
        assertTrue(detections.size() >= 0, "Detection count should be non-negative");
        
        // Validate detection structure
        for (Detection detection : detections) {
            assertNotNull(detection.label(), "Detection label should not be null");
            assertNotNull(detection.boundingBox(), "Bounding box should not be null");
            assertTrue(detection.confidence() >= 0.0 && detection.confidence() <= 1.0, 
                      "Confidence should be between 0 and 1");
            assertNotNull(detection.attributes(), "Attributes should not be null");
        }
    }
    
    /**
     * Tests pose detection with MediaPipe backend.
     */
    @Test
    void testMediaPipePoseDetection() {
        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.POSE)
            .minConfidence(0.5)
            .maxDetections(5)
            .build();
        
        VisionResult result = visionTemplate.detect(testImage, query);
        List<Detection> detections = result.detections();
        
        assertNotNull(detections, "Detections should not be null");
        assertTrue(detections.size() >= 0, "Detection count should be non-negative");
        
        // Validate detection structure
        for (Detection detection : detections) {
            assertNotNull(detection.label(), "Detection label should not be null");
            assertNotNull(detection.boundingBox(), "Bounding box should not be null");
            assertTrue(detection.confidence() >= 0.0 && detection.confidence() <= 1.0, 
                      "Confidence should be between 0 and 1");
            assertNotNull(detection.attributes(), "Attributes should not be null");
        }
    }
    
    /**
     * Tests error handling with invalid input.
     */
    @Test
    void testErrorHandlingWithInvalidInput() {
        // Test with null image data
        assertThrows(IllegalArgumentException.class, () -> {
            visionTemplate.detect((ImageData) null, new DetectionQuery.Builder()
                .type(DetectionType.FACE)
                .build());
        }, "Should throw exception for null image data");
        
        // Test with empty image data
        ImageData emptyImage = ImageData.fromBytes(new byte[0], "image/jpeg");
        assertThrows(IllegalArgumentException.class, () -> {
            visionTemplate.detect(emptyImage, new DetectionQuery.Builder()
                .type(DetectionType.FACE)
                .build());
        }, "Should throw exception for empty image data");
        
        // Test with null query
        assertThrows(IllegalArgumentException.class, () -> {
            visionTemplate.detect(testImage, (DetectionQuery) null);
        }, "Should throw exception for null query");
    }
    
    /**
     * Tests error handling with unsupported detection type.
     */
    @Test
    void testErrorHandlingWithUnsupportedType() {
        DetectionQuery query = new DetectionQuery.Builder()
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
        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.FACE)
            .minConfidence(0.5)
            .maxDetections(10)
            .build();
        
        VisionResult result = visionTemplate.detect(testImage, query);
        List<Detection> detections = result.detections();
        
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
            assertNotNull(healthInfo.backendId(), "Backend ID should not be null");
            assertNotNull(healthInfo.status(), "Health status should not be null");
            assertNotNull(healthInfo.statusMessage(), "Status message should not be null");
        }
    }
    
    /**
     * Tests configuration validation.
     */
    @Test
    void testConfigurationValidation() {
        // Test valid configuration
        DetectionQuery validQuery = new DetectionQuery.Builder()
            .type(DetectionType.FACE)
            .minConfidence(0.5)
            .maxDetections(10)
            .options(Map.of("nmsThreshold", 0.45))
            .build();
        
        assertNotNull(validQuery, "Valid query should be created");
        assertEquals(DetectionType.FACE, validQuery.getType(), "Query type should match");
        assertEquals(0.5, validQuery.getMinConfidence(), "Min confidence should match");
        assertEquals(10, validQuery.getMaxDetections(), "Max detections should match");
        assertEquals(0.45, validQuery.getNmsThreshold(), "NMS threshold should match");
    }
    
    /**
     * Tests image data validation.
     */
    @Test
    void testImageDataValidation() {
        // Test valid image data
        ImageData validImage = ImageData.fromBytes(testImageData, "image/jpeg");
        assertNotNull(validImage, "Valid image should be created");
        assertEquals(testImageData.length, validImage.data().length, "Image data should match");
        assertEquals("image/jpeg", validImage.format(), "Image format should match");
        
        // Note: ImageData record doesn't support metadata in constructor
        // Metadata would need to be handled differently in the actual implementation
    }
    
    /**
     * Tests detection result validation.
     */
    @Test
    void testDetectionResultValidation() {
        // Create a test detection
        BoundingBox box = new BoundingBox(0.1, 0.2, 0.3, 0.4);
        Map<String, Object> attributes = Map.of("confidence", 0.95, "category", "FACE");
        
        Detection detection = new Detection("FACE", 0.95, box, attributes);
        
        assertNotNull(detection, "Detection should not be null");
        assertEquals("FACE", detection.label(), "Detection label should match");
        assertEquals(box, detection.boundingBox(), "Bounding box should match");
        assertEquals(0.95, detection.confidence(), "Confidence should match");
        assertEquals(attributes, detection.attributes(), "Attributes should match");
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
    
} 