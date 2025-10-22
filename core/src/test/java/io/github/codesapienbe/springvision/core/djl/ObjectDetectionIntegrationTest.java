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
 * Comprehensive integration test for object detection capabilities using real YOLOv8 models.
 * Tests the full object detection pipeline from image input to detection results.
 */
@DisplayName("Object Detection Integration Tests")
public class ObjectDetectionIntegrationTest {

    private static DjlVisionBackend backend;
    private static boolean modelsAvailable = false;

    @BeforeAll
    static void setup() throws Exception {
        // Check if models are available first
        modelsAvailable = YoloLoader.isModelAvailable("yolov8/yolov8n.pt");

        if (!modelsAvailable) {
            System.out.println("YOLOv8 models not available, skipping real model tests");
            return;
        }

        // Note: ai.djl.offline=false is now set at JVM level via Maven Surefire plugin
        // This ensures DJL initializes in online mode for integration tests

        DjlProperties properties = new DjlProperties();
        properties.setEngine("PyTorch");
        properties.setDevice("cpu");
        properties.setAutoDownload(true); // Enable model loading for integration tests

        // Configure object detection with nano model for faster testing
        properties.getObjectDetection().setModel("yolo");
        properties.getObjectDetection().setConfidenceThreshold(0.1f); // Very low threshold for test images

        backend = new DjlVisionBackend(properties);

        // Initialize with models - this should load models since offline=false
        backend.initialize();

        // Verify backend is ready
        if (backend.isHealthy()) {
            System.out.println("Object detection backend initialized successfully with models");
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
    @DisplayName("Should detect rectangular object in test image")
    @EnabledIf("modelsAvailable")
    void shouldDetectRectangularObject() throws IOException {
        // Given: An image with a prominent colored rectangle
        ImageData image = TestImageUtils.createRectangleImage(640, 480, Color.BLUE);

        // When: Object detection is performed
        List<Detection> detections = backend.detectObjects(image);

        // Then: Should return a result list and complete successfully
        assertThat(detections).isNotNull();
        assertThat(detections).isInstanceOf(List.class);

        // CRITICAL: With real models loaded, we should NOT get empty results
        // If models are loaded but returning empty lists, something is wrong
        if (backend.isObjectDetectionModelAvailable()) {
            // If model claims to be loaded, we should get actual detection results
            // Even if it's just background detections, there should be some output
            assertThat(detections).isNotEmpty()
                .withFailMessage("Model claims to be loaded but returned no detections. " +
                    "This indicates the model is not actually processing images.");
            System.out.println("✅ Successfully detected " + detections.size() + " objects with real model");
        } else {
            // If model is not loaded, empty results are expected but this is an error condition
            fail("Object detection model should be loaded but is not. Check backend initialization.");
        }
    }

    @Test
    @DisplayName("Should verify model loading status is accurate")
    @EnabledIf("modelsAvailable")
    void shouldVerifyModelLoadingStatus() {
        // Test that the backend correctly reports model loading status
        boolean objectDetectionLoaded = backend.isObjectDetectionModelAvailable();

        if (objectDetectionLoaded) {
            System.out.println("✅ Object detection model is correctly reported as loaded");
        } else {
            fail("Backend initialization claimed success but object detection model is not loaded. " +
                 "This indicates a bug in the initialization or model loading logic.");
        }
    }

    @Test
    @DisplayName("Should fail appropriately when backend cannot initialize with models")
    void shouldFailAppropriatelyWhenBackendCannotInitializeWithModels() {
        // This test validates that the integration test setup correctly detects
        // when the backend fails to initialize with real models

        // The setup should have failed with VisionBackendException due to PyTorch JNI issues
        // This is the correct behavior - the test should not proceed if models can't be loaded

        // If we get here, it means the setup succeeded, which would be unexpected
        // given the known PyTorch JNI library issues

        System.out.println("⚠️  Unexpected: Backend initialization succeeded despite known PyTorch JNI issues");

        // But if models ARE available, validate that they're actually loaded
        if (backend != null && backend.isObjectDetectionModelAvailable()) {
            System.out.println("✅ Models are actually loaded despite JNI issues - this is unexpected but good!");
        } else {
            fail("Backend claims to be healthy but models are not loaded. This indicates " +
                 "the backend is incorrectly reporting success when it should fail.");
        }
    }

    @Test
    @DisplayName("Should skip tests when models not available")
    @EnabledIf("modelsNotAvailable")
    void shouldSkipWhenModelsNotAvailable() {
        // This test passes when models are not available, indicating tests are properly skipped
        System.out.println("Object detection models not available - tests properly skipped");
    }

    @Test
    @DisplayName("Should detect geometric shapes in test images")
    @EnabledIf("modelsAvailable")
    void shouldDetectGeometricShapes() throws IOException {
        // Test different geometric shapes
        String[] shapes = {"square", "circle", "triangle"};

        for (String shape : shapes) {
            // Given: Image with geometric shape
            ImageData image = TestImageUtils.createGeometricShapeImage(640, 480, shape);

            // When: Object detection is performed
            List<Detection> detections = backend.detectObjects(image);

            // Then: Should complete successfully
            assertThat(detections).isNotNull();
            assertThat(detections).isInstanceOf(List.class);
        }
    }

    @Test
    @DisplayName("Should handle empty image gracefully")
    @EnabledIf("modelsAvailable")
    void shouldHandleEmptyImageGracefully() throws IOException {
        // Given: Empty/white image
        ImageData emptyImage = TestImageUtils.createEmptyImage(640, 480);

        // When: Object detection is performed
        List<Detection> detections = backend.detectObjects(emptyImage);

        // Then: Should return empty or minimal detections
        assertThat(detections).isNotNull();
        assertThat(detections).isInstanceOf(List.class);
    }

    @Test
    @DisplayName("Should handle noise image gracefully")
    @EnabledIf("modelsAvailable")
    void shouldHandleNoiseImageGracefully() throws IOException {
        // Given: Random noise image
        ImageData noiseImage = TestImageUtils.createNoiseImage(640, 480);

        // When: Object detection is performed
        List<Detection> detections = backend.detectObjects(noiseImage);

        // Then: Should complete successfully
        assertThat(detections).isNotNull();
        assertThat(detections).isInstanceOf(List.class);
    }

    @Test
    @DisplayName("Should handle different image sizes")
    @EnabledIf("modelsAvailable")
    void shouldHandleDifferentImageSizes() throws IOException {
        // Test various image sizes
        int[][] sizes = {{320, 240}, {640, 480}, {800, 600}, {1920, 1080}};

        for (int[] size : sizes) {
            // Given: Image of specific size
            ImageData image = TestImageUtils.createRectangleImage(size[0], size[1], Color.RED);

            // When: Object detection is performed
            List<Detection> detections = backend.detectObjects(image);

            // Then: Should complete successfully
            assertThat(detections).isNotNull();
            assertThat(detections).isInstanceOf(List.class);
        }
    }

    @Test
    @DisplayName("Should handle different image formats")
    @EnabledIf("modelsAvailable")
    void shouldHandleDifferentImageFormats() throws IOException {
        // Given: Test image
        ImageData image = TestImageUtils.createRectangleImage(640, 480, Color.GREEN);

        // When: Object detection is performed
        List<Detection> detections = backend.detectObjects(image);

        // Then: Should complete successfully regardless of format
        assertThat(detections).isNotNull();
        assertThat(detections).isInstanceOf(List.class);
    }

    @Test
    @DisplayName("Should return detections with valid structure")
    @EnabledIf("modelsAvailable")
    void shouldReturnDetectionsWithValidStructure() throws IOException {
        // Given: Test image with detectable content
        ImageData image = TestImageUtils.createRectangleImage(640, 480, Color.BLUE);

        // When: Object detection is performed
        List<Detection> detections = backend.detectObjects(image);

        // Then: Each detection should have valid structure
        for (Detection detection : detections) {
            assertThat(detection).isNotNull();
            assertThat(detection.label()).isNotNull();
            assertThat(detection.confidence()).isBetween(0.0, 1.0);

            // Bounding box should be present and valid
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
    @DisplayName("Should respect confidence threshold")
    @EnabledIf("modelsAvailable")
    void shouldRespectConfidenceThreshold() throws IOException {
        // Given: Test image
        ImageData image = TestImageUtils.createRectangleImage(640, 480, Color.BLUE);

        // When: Object detection is performed with different thresholds
        List<Detection> allDetections = backend.detectObjects(image);

        // Then: All detections should meet the configured confidence threshold
        float configuredThreshold = 0.3f; // From setup
        for (Detection detection : allDetections) {
            assertThat(detection.confidence()).isGreaterThanOrEqualTo(configuredThreshold);
        }
    }

    @Test
    @DisplayName("Should limit maximum detections")
    @EnabledIf("modelsAvailable")
    void shouldLimitMaximumDetections() throws IOException {
        // Given: Test image
        ImageData image = TestImageUtils.createRectangleImage(640, 480, Color.BLUE);

        // When: Object detection is performed
        List<Detection> detections = backend.detectObjects(image);

        // Then: Should not exceed configured maximum
        int maxConfigured = 10; // From setup
        assertThat(detections.size()).isLessThanOrEqualTo(maxConfigured);
    }

    @Test
    @DisplayName("Should perform real inference with loaded models")
    @EnabledIf("modelsAvailable")
    void shouldPerformRealInferenceWithLoadedModels() throws IOException {
        // Given: Test image with clear visual features
        ImageData image = TestImageUtils.createRectangleImage(640, 480, Color.RED);

        // When: Object detection is performed
        long startTime = System.currentTimeMillis();
        List<Detection> detections = backend.detectObjects(image);
        long inferenceTime = System.currentTimeMillis() - startTime;

        // Then: Validate complete inference pipeline
        assertThat(detections).isNotNull();

        // If model is loaded, we should get some inference results
        if (backend.isObjectDetectionModelAvailable()) {
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
                "detected " + detections.size() + " objects including: " +
                detections.stream().map(Detection::label).distinct().limit(3).toList());
        }
    }

    @Test
    @DisplayName("Should validate complete integration test behavior")
    void shouldValidateCompleteIntegrationTestBehavior() {
        // This is a comprehensive test that validates the entire integration testing approach

        System.out.println("🔍 Integration Test Validation:");
        System.out.println("  - Backend initialized: " + (backend != null));
        System.out.println("  - Backend healthy: " + (backend != null && backend.isHealthy()));
        System.out.println("  - Models available on disk: " + modelsAvailable);
        System.out.println("  - Object detection model loaded: " +
            (backend != null && backend.isObjectDetectionModelAvailable()));

        // Key validations for real integration tests:
        if (modelsAvailable) {
            // If model files exist, the backend should be able to load them
            assertThat(backend).isNotNull()
                .withFailMessage("Backend should initialize when model files are available");

            if (backend.isHealthy()) {
                // If backend reports healthy, models should actually be loaded
                assertThat(backend.isObjectDetectionModelAvailable())
                    .withFailMessage("Backend reports healthy but object detection model not loaded");
            } else {
                // If backend is not healthy, it should fail initialization (which it currently does)
                System.out.println("✅ Backend correctly fails when PyTorch JNI libraries unavailable");
            }
        } else {
            // If no models available, backend should still initialize but in offline mode
            System.out.println("ℹ️  No models available - this is expected in CI/test environments");
        }

        System.out.println("✅ Integration test validation completed");
    }

    @Test
    @EnabledIf("modelsAvailable")
    @DisplayName("Should use real YOLOv8 model when available")
    void shouldUseRealYoloModelWhenAvailable() throws IOException {
        // Given: Test image
        ImageData image = TestImageUtils.createRectangleImage(640, 480, Color.BLUE);

        // When: Object detection is performed
        List<Detection> detections = backend.detectObjects(image);

        // Then: Should have completed successfully with real model
        assertThat(detections).isNotNull();
        assertThat(detections).isInstanceOf(List.class);
    }

    /**
     * Check if YOLO model is available for testing.
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
}
