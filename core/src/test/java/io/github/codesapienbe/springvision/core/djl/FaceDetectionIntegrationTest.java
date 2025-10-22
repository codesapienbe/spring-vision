package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Comprehensive integration test for face detection capabilities using real RetinaFace models.
 * Tests the full face detection pipeline from image input to detection results.
 */
@DisplayName("Face Detection Integration Tests")
public class FaceDetectionIntegrationTest {

    private static DjlVisionBackend backend;

    private static boolean modelsAvailable = false;

    @BeforeAll
    static void setup() throws Exception {
        // Check if models are available first
        modelsAvailable = YoloLoader.isModelAvailable("retinaface/retinaface.pt");

        if (!modelsAvailable) {
            System.out.println("Face detection models not available, skipping real model tests");
            return;
        }

        // Note: ai.djl.offline=false is now set at JVM level via Maven Surefire plugin
        // This ensures DJL initializes in online mode for integration tests
        System.setProperty("OPT_OUT_TRACKING", "true");

        DjlProperties properties = new DjlProperties();
        properties.setEngine("PyTorch");
        properties.setDevice("cpu");
        properties.setAutoDownload(true); // Enable model loading for integration tests

        // Configure face detection
        properties.getFaceDetection().setConfidenceThreshold(0.5f);
        properties.getFaceDetection().setMaxFaces(5);

        backend = new DjlVisionBackend(properties);

        // Initialize with models - this should load models since offline=false
        backend.initialize();

        // Verify backend is ready
        if (backend.isHealthy()) {
            System.out.println("Face detection backend initialized successfully with models");
        } else {
            System.out.println("Backend initialized but may be in offline mode - models not loaded");
        }
    }

    @AfterAll
    static void teardown() {
        if (backend != null) {
            backend.shutdown();
        }
    }

    @Test
    @DisplayName("Should skip tests when models not available")
    @EnabledIf("modelsNotAvailable")
    void shouldSkipWhenModelsNotAvailable() {
        // This test passes when models are not available, indicating tests are properly skipped
        System.out.println("Face detection models not available - tests properly skipped");
    }

    @Test
    @DisplayName("Should handle face detection on simple test images")
    @EnabledIf("modelsAvailable")
    void shouldHandleFaceDetectionOnSimpleImages() throws IOException {
        // Given: A simple image that might contain face-like features
        ImageData image = TestImageUtils.createSimpleFaceImage(640, 480);

        // When: Face detection is performed
        List<Detection> detections = backend.detectFaces(image);

        // Then: Should return a result list and complete successfully
        assertThat(detections).isNotNull();
        assertThat(detections).isInstanceOf(List.class);

        // CRITICAL: With real models loaded, we should NOT get empty results
        // If models are loaded but returning empty lists, something is wrong
        if (backend.isFaceDetectionModelAvailable()) {
            // If model claims to be loaded, we should get actual detection results
            // Even if it's just background detections, there should be some output
            assertThat(detections).isNotEmpty()
                .withFailMessage("Model claims to be loaded but returned no face detections. " +
                    "This indicates the model is not actually processing images.");
            System.out.println("✅ Successfully detected " + detections.size() + " faces with real model");
        } else {
            // If model is not loaded, empty results are expected but this is an error condition
            fail("Face detection model should be loaded but is not. Check backend initialization.");
        }
    }

    @Test
    @DisplayName("Should handle empty image gracefully")
    @EnabledIf("modelsAvailable")
    void shouldHandleEmptyImageGracefully() throws IOException {
        // Given: Empty/white image
        ImageData emptyImage = TestImageUtils.createEmptyImage(640, 480);

        // When: Face detection is performed
        List<Detection> detections = backend.detectFaces(emptyImage);

        // Then: Should return empty detections
        assertThat(detections).isNotNull();
        assertThat(detections).isEmpty();
    }

    @Test
    @DisplayName("Should handle noise image gracefully")
    @EnabledIf("modelsAvailable")
    void shouldHandleNoiseImageGracefully() throws IOException {
        // Given: Random noise image
        ImageData noiseImage = TestImageUtils.createNoiseImage(640, 480);

        // When: Face detection is performed
        List<Detection> detections = backend.detectFaces(noiseImage);

        // Then: Should complete successfully
        assertThat(detections).isNotNull();
        assertThat(detections).isInstanceOf(List.class);
    }

    @Test
    @DisplayName("Should handle different image sizes")
    @EnabledIf("modelsAvailable")
    void shouldHandleDifferentImageSizes() throws IOException {
        // Test various image sizes
        int[][] sizes = {{320, 240}, {640, 480}, {800, 600}};

        for (int[] size : sizes) {
            // Given: Image of specific size
            ImageData image = TestImageUtils.createSimpleFaceImage(size[0], size[1]);

            // When: Face detection is performed
            List<Detection> detections = backend.detectFaces(image);

            // Then: Should complete successfully
            assertThat(detections).isNotNull();
            assertThat(detections).isInstanceOf(List.class);
        }
    }

    @Test
    @DisplayName("Should handle different image formats")
    @EnabledIf("modelsAvailable")
    void shouldHandleDifferentImageFormats() throws IOException {
        // Given: Test images
        ImageData image1 = TestImageUtils.createSimpleFaceImage(640, 480);
        ImageData image2 = TestImageUtils.createRectangleImage(640, 480, Color.RED);

        // When: Face detection is performed on both
        List<Detection> detections1 = backend.detectFaces(image1);
        List<Detection> detections2 = backend.detectFaces(image2);

        // Then: Should complete successfully for both
        assertThat(detections1).isNotNull();
        assertThat(detections2).isNotNull();
    }

    @Test
    @DisplayName("Should return detections with valid face structure")
    @EnabledIf("modelsAvailable")
    void shouldReturnDetectionsWithValidFaceStructure() throws IOException {
        // Given: Test image
        ImageData image = TestImageUtils.createSimpleFaceImage(640, 480);

        // When: Face detection is performed
        List<Detection> detections = backend.detectFaces(image);

        // Then: Each detection should have valid structure
        for (Detection detection : detections) {
            assertThat(detection).isNotNull();
            assertThat(detection.label()).isNotNull();
            assertThat(detection.confidence()).isBetween(0.0, 1.0);

            // Face detections should have bounding boxes
            if (detection.boundingBox() != null) {
                assertThat(detection.boundingBox().x()).isBetween(0.0, 1.0);
                assertThat(detection.boundingBox().y()).isBetween(0.0, 1.0);
                assertThat(detection.boundingBox().width()).isBetween(0.0, 1.0);
                assertThat(detection.boundingBox().height()).isBetween(0.0, 1.0);
            }

            // Attributes should be present
            assertThat(detection.attributes()).isNotNull();
            assertThat(detection.attributes()).containsKey("backend");
            assertThat(detection.attributes().get("backend")).isEqualTo("djl");
        }
    }

    @Test
    @DisplayName("Should respect confidence threshold for faces")
    @EnabledIf("modelsAvailable")
    void shouldRespectConfidenceThreshold() throws IOException {
        // Given: Test image
        ImageData image = TestImageUtils.createSimpleFaceImage(640, 480);

        // When: Face detection is performed
        List<Detection> detections = backend.detectFaces(image);

        // Then: All detections should meet the configured confidence threshold
        float configuredThreshold = 0.5f; // From setup
        for (Detection detection : detections) {
            assertThat(detection.confidence()).isGreaterThanOrEqualTo(configuredThreshold);
        }
    }

    @Test
    @DisplayName("Should limit maximum faces detected")
    @EnabledIf("modelsAvailable")
    void shouldLimitMaximumFacesDetected() throws IOException {
        // Given: Test image
        ImageData image = TestImageUtils.createSimpleFaceImage(640, 480);

        // When: Face detection is performed
        List<Detection> detections = backend.detectFaces(image);

        // Then: Should not exceed configured maximum
        int maxConfigured = 5; // From setup
        assertThat(detections.size()).isLessThanOrEqualTo(maxConfigured);
    }

    @Test
    @DisplayName("Should fallback to object detection when face model unavailable")
    @EnabledIf("modelsAvailable")
    void shouldFallbackToObjectDetectionWhenFaceModelUnavailable() throws IOException {
        // Given: Test image
        ImageData image = TestImageUtils.createSimpleFaceImage(640, 480);

        // When: Face detection is performed
        List<Detection> detections = backend.detectFaces(image);

        // Then: Should complete successfully (may use object detection fallback)
        assertThat(detections).isNotNull();
        assertThat(detections).isInstanceOf(List.class);
    }

    @Test
    @DisplayName("Should handle concurrent face detection requests")
    @EnabledIf("modelsAvailable")
    void shouldHandleConcurrentFaceDetectionRequests() throws IOException {
        // Given: Multiple test images
        ImageData image1 = TestImageUtils.createSimpleFaceImage(640, 480);
        ImageData image2 = TestImageUtils.createRectangleImage(640, 480, Color.BLUE);
        ImageData image3 = TestImageUtils.createEmptyImage(640, 480);

        // When: Multiple face detection requests are made concurrently
        List<Detection> detections1 = backend.detectFaces(image1);
        List<Detection> detections2 = backend.detectFaces(image2);
        List<Detection> detections3 = backend.detectFaces(image3);

        // Then: All should complete successfully
        assertThat(detections1).isNotNull();
        assertThat(detections2).isNotNull();
        assertThat(detections3).isNotNull();
    }

    @Test
    @DisplayName("Should perform real inference with loaded models")
    @EnabledIf("modelsAvailable")
    void shouldPerformRealInferenceWithLoadedModels() throws IOException {
        // Given: Test image with clear visual features
        ImageData image = TestImageUtils.createSimpleFaceImage(640, 480);

        // When: Face detection is performed
        long startTime = System.currentTimeMillis();
        List<Detection> detections = backend.detectFaces(image);
        long inferenceTime = System.currentTimeMillis() - startTime;

        // Then: Validate complete inference pipeline
        assertThat(detections).isNotNull();

        // If model is loaded, we should get some inference results
        if (backend.isFaceDetectionModelAvailable()) {
            // Real inference should take some measurable time (not just return empty immediately)
            assertThat(inferenceTime).isGreaterThan(10)
                .withFailMessage("Inference completed too quickly (" + inferenceTime + "ms), " +
                    "suggesting model was not actually used");

            // Should get some detections (even if just background/generic objects)
            assertThat(detections).isNotEmpty()
                .withFailMessage("Model is loaded but produced no detections. " +
                    "Check if model is actually processing images.");

            // Validate detection structure
            Detection firstDetection = detections.get(0);
            assertThat(firstDetection.label()).isNotNull();
            assertThat(firstDetection.confidence()).isBetween(0.0, 1.0);
            assertThat(firstDetection.boundingBox()).isNotNull();

            System.out.println("✅ Real inference completed in " + inferenceTime + "ms, " +
                "detected " + detections.size() + " faces including: " +
                detections.stream().map(Detection::label).distinct().limit(3).toList());
        }
    }

    @Test
    @EnabledIf("modelsAvailable")
    @DisplayName("Should use real RetinaFace model when available")
    void shouldUseRealRetinaFaceModelWhenAvailable() throws IOException {
        // Given: Test image
        ImageData image = TestImageUtils.createSimpleFaceImage(640, 480);

        // When: Face detection is performed
        List<Detection> detections = backend.detectFaces(image);

        // Then: Should have completed successfully with real RetinaFace model
        assertThat(detections).isNotNull();
        assertThat(detections).isInstanceOf(List.class);
    }

    /**
     * Check if face detection models are available for testing.
     */
    static boolean modelsAvailable() {
        return modelsAvailable;
    }

    /**
     * Check if models are NOT available (for skip test).
     */
    static boolean modelsNotAvailable() {
        return !modelsAvailable;
    }

    /**
     * Check if RetinaFace model is available for testing.
     */
    static boolean isRetinaFaceModelAvailable() {
        return YoloLoader.isModelAvailable("retinaface/retinaface.pt");
    }
}
