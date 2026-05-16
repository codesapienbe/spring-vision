package io.github.codesapienbe.springvision.core.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;

import io.github.codesapienbe.springvision.core.*;
import io.github.codesapienbe.springvision.core.djl.DjlProperties;
import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;
import io.github.codesapienbe.springvision.core.djl.YoloLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Comprehensive integration test for FaceDetectionCapability using real RetinaFace models offline.
 * <p>
 * This test focuses on the essential face detection functions and ensures they work
 * with actual PyTorch models without synthetic fallbacks.
 * <p>
 * Key requirements:
 * - Uses actual RetinaFace model offline (no network downloads)
 * - Tests core face detection functionality
 * - Validates model loading and inference
 * - Ensures consistent PyTorch engine usage
 */
@DisplayName("Face Detection Capability Integration Test")
public class FaceDetectionCapabilityIntegrationTest {

    private static DjlVisionBackend backend;
    private static boolean retinaFaceModelAvailable = false;

    @BeforeAll
    static void setup() throws Exception {
        // Check if RetinaFace model is available offline
        retinaFaceModelAvailable = YoloLoader.isModelAvailable("retinaface/retinaface.pt");

        if (!retinaFaceModelAvailable) {
            System.out.println("RetinaFace model not available offline - skipping real model tests");
            return;
        }

        // Configure for offline testing with PyTorch engine
        System.setProperty("ai.djl.offline", "true"); // Force offline mode
        System.setProperty("OPT_OUT_TRACKING", "true");

        DjlProperties properties = new DjlProperties();
        properties.setEngine("PyTorch"); // Ensure PyTorch engine
        properties.setDevice("cpu");
        properties.setAutoDownload(false); // No downloads in offline mode

        // Configure face detection parameters
        properties.getFaceDetection().setConfidenceThreshold(0.5f);
        properties.getFaceDetection().setMaxFaces(10);

        backend = new DjlVisionBackend(properties);

        // Initialize backend - should load RetinaFace model offline
        backend.initialize();

        // Verify backend health and model availability
        assertThat(backend.isHealthy())
            .withFailMessage("Backend should be healthy after initialization")
            .isTrue();

        assertThat(backend.isFaceDetectionModelAvailable())
            .withFailMessage("RetinaFace model should be available offline")
            .isTrue();

        System.out.println("Face Detection Capability integration test initialized successfully");
        System.out.println("- Backend: " + backend.getDisplayName());
        System.out.println("- Engine: PyTorch");
        System.out.println("- Face Detection Model: Available offline");
    }

    @AfterAll
    static void teardown() {
        if (backend != null) {
            backend.shutdown();
        }
        // Reset system properties
        System.clearProperty("ai.djl.offline");
    }

    @Test
    @DisplayName("Should skip all tests when RetinaFace model not available offline")
    @EnabledIf("retinaFaceModelNotAvailable")
    void shouldSkipWhenModelNotAvailable() {
        System.out.println("RetinaFace model not available offline - tests properly skipped");
    }

    @Test
    @DisplayName("Should initialize backend with RetinaFace model offline")
    @EnabledIf("retinaFaceModelAvailable")
    void shouldInitializeWithRetinaFaceModelOffline() {
        // Verify backend metadata
        assertThat(backend.getBackendId()).isEqualTo("djl");
        assertThat(backend.getDisplayName()).isEqualTo("DJL Vision Backend");
        assertThat(backend.getVersion()).contains("DJL");

        // Verify supported detection types include FACE
        assertThat(backend.getSupportedDetectionTypes())
            .contains(DetectionType.FACE);

        // Verify backend health
        assertThat(backend.isHealthy()).isTrue();

        // Verify face detection model availability
        assertThat(backend.isFaceDetectionModelAvailable()).isTrue();
    }

    @Test
    @DisplayName("Should perform face detection with default parameters")
    @EnabledIf("retinaFaceModelAvailable")
    void shouldPerformFaceDetectionWithDefaults() throws IOException {
        // Given: A test image with face-like features
        ImageData testImage = TestImageUtils.createSimpleFaceImage(640, 480);

        // When: Face detection is called with default parameters
        List<Detection> detections = backend.detectFaces(testImage);

        // Then: Should return valid results
        assertThat(detections).isNotNull();
        assertThat(detections).isInstanceOf(List.class);

        // With real RetinaFace model, we should get actual detection results
        // (may be empty if no faces detected, but should not be null)
        for (Detection detection : detections) {
            assertThat(detection.label()).isNotNull();
            assertThat(detection.confidence()).isBetween(0.0, 1.0);
            assertThat(detection.boundingBox()).isNotNull();
        }
    }

    @Test
    @DisplayName("Should perform face detection with custom query parameters")
    @EnabledIf("retinaFaceModelAvailable")
    void shouldPerformFaceDetectionWithCustomQuery() throws IOException {
        // Given: A test image and custom detection query
        ImageData testImage = TestImageUtils.createSimpleFaceImage(640, 480);

        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.FACE)
            .minConfidence(0.7f)
            .maxDetections(3)
            .build();

        // When: Face detection is called with custom query
        List<Detection> detections = backend.detectFaces(testImage, query);

        // Then: Results should respect query parameters
        assertThat(detections).isNotNull();

        for (Detection detection : detections) {
            // All detections should meet minimum confidence threshold
            assertThat(detection.confidence())
                .isGreaterThanOrEqualTo(query.getMinConfidence());

            // Should not exceed max detections limit
            assertThat(detections.size())
                .isLessThanOrEqualTo(query.getMaxDetections());
        }
    }

    @Test
    @DisplayName("Should handle various image formats and sizes")
    @EnabledIf("retinaFaceModelAvailable")
    void shouldHandleVariousImageFormatsAndSizes() throws IOException {
        // Test different image sizes
        int[][] testSizes = {{320, 240}, {640, 480}, {800, 600}, {1024, 768}};

        for (int[] size : testSizes) {
            ImageData testImage = TestImageUtils.createSimpleFaceImage(size[0], size[1]);

            // Should not throw exceptions for different sizes
            List<Detection> detections = backend.detectFaces(testImage);

            assertThat(detections).isNotNull();
            System.out.println("Successfully processed " + size[0] + "x" + size[1] + " image: " + detections.size() + " detections");
        }
    }

    @Test
    @DisplayName("Should validate input parameters")
    @EnabledIf("retinaFaceModelAvailable")
    void shouldValidateInputParameters() {
        // Test null image data
        assertThatThrownBy(() -> backend.detectFaces(null))
            .isInstanceOf(BaseVisionException.class);

        // Test empty image data
        ImageData emptyImage = ImageData.fromBytes(new byte[0], "image/jpeg");
        assertThatThrownBy(() -> backend.detectFaces(emptyImage))
            .isInstanceOf(BaseVisionException.class);

        // Test oversized image
        byte[] largeData = new byte[100 * 1024 * 1024]; // 100MB
        ImageData largeImage = ImageData.fromBytes(largeData, "image/jpeg");
        assertThatThrownBy(() -> backend.detectFaces(largeImage))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should provide consistent results across multiple calls")
    @EnabledIf("retinaFaceModelAvailable")
    void shouldProvideConsistentResults() throws IOException {
        // Given: Same test image
        ImageData testImage = TestImageUtils.createSimpleFaceImage(640, 480);

        // When: Multiple detection calls on same image
        List<Detection> result1 = backend.detectFaces(testImage);
        List<Detection> result2 = backend.detectFaces(testImage);
        List<Detection> result3 = backend.detectFaces(testImage);

        // Then: Results should be consistent (same number of detections)
        assertThat(result1.size()).isEqualTo(result2.size());
        assertThat(result2.size()).isEqualTo(result3.size());

        // Note: Exact confidence values may vary slightly due to floating point precision,
        // but the overall pattern should be consistent
    }

    @Test
    @DisplayName("Should handle backend health checks")
    @EnabledIf("retinaFaceModelAvailable")
    void shouldHandleBackendHealthChecks() {
        // Backend should report healthy status
        assertThat(backend.isHealthy()).isTrue();

        // Health info should contain relevant details
        var healthInfo = backend.getHealthInfo();
        assertThat(healthInfo).isNotNull();
        assertThat(healthInfo.status()).isEqualTo(BackendHealthInfo.HealthStatus.HEALTHY);

        // Health details should include engine and model information
        @SuppressWarnings("unchecked")
        var details = healthInfo.metrics();
        assertThat(details).containsKey("engine");
        assertThat(details).containsKey("modelsLoaded");
        assertThat(details.get("engine")).isEqualTo("PyTorch");
    }

    @Test
    @DisplayName("Should properly handle backend lifecycle")
    @EnabledIf("retinaFaceModelAvailable")
    void shouldHandleBackendLifecycle() throws Exception {
        // Backend should be healthy after initialization
        assertThat(backend.isHealthy()).isTrue();

        // Should be able to perform detections
        ImageData testImage = TestImageUtils.createSimpleFaceImage(640, 480);
        List<Detection> detections = backend.detectFaces(testImage);
        assertThat(detections).isNotNull();

        // After shutdown, backend should not be healthy
        backend.shutdown();
        assertThat(backend.isHealthy()).isFalse();

        // Re-initialize for other tests
        backend.initialize();
        assertThat(backend.isHealthy()).isTrue();
    }

    // Helper methods for conditional test execution

    static boolean retinaFaceModelAvailable() {
        return retinaFaceModelAvailable;
    }

    static boolean retinaFaceModelNotAvailable() {
        return !retinaFaceModelAvailable;
    }
}
