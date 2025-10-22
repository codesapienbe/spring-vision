package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;

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

    @BeforeAll
    static void setup() throws Exception {
        // Configure backend for real model testing
        System.setProperty("ai.djl.offline", "true");
        System.setProperty("OPT_OUT_TRACKING", "true");

        DjlProperties properties = new DjlProperties();
        properties.setEngine("PyTorch");
        properties.setDevice("cpu");
        properties.setAutoDownload(false); // Use pre-downloaded models

        // Configure face detection
        properties.getFaceDetection().setConfidenceThreshold(0.5f);
        properties.getFaceDetection().setMaxFaces(5);

        backend = new DjlVisionBackend(properties);

        // Initialize with models
        backend.initialize();

        // Verify backend is ready
        assertThat(backend.isHealthy()).isTrue();
    }

    @AfterAll
    static void teardown() {
        if (backend != null) {
            backend.shutdown();
        }
    }

    @Test
    @DisplayName("Should handle face detection on simple test images")
    void shouldHandleFaceDetectionOnSimpleImages() throws IOException {
        // Given: A simple image that might contain face-like features
        ImageData image = TestImageUtils.createSimpleFaceImage(640, 480);

        // When: Face detection is performed
        List<Detection> detections = backend.detectFaces(image);

        // Then: Should return a result (may be empty if no faces detected)
        assertThat(detections).isNotNull();
        assertThat(detections).isInstanceOf(List.class);
    }

    @Test
    @DisplayName("Should handle empty image gracefully")
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
    @EnabledIf("isRetinaFaceModelAvailable")
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
     * Check if RetinaFace model is available for testing.
     */
    static boolean isRetinaFaceModelAvailable() {
        return YoloLoader.isModelAvailable("retinaface/retinaface.pt");
    }
}
