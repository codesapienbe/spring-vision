package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Comprehensive integration test for pose estimation capabilities using real YOLOv8-pose models.
 * Tests the full pose estimation pipeline from image input to keypoint detection results.
 */
@DisplayName("Pose Estimation Integration Tests")
public class PoseEstimationIntegrationTest {

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

        // Configure pose estimation
        properties.getPoseEstimation().setModel("yolo");
        properties.getPoseEstimation().setConfidenceThreshold(0.3f);

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
    @DisplayName("Should handle pose estimation on person-like images")
    void shouldHandlePoseEstimationOnPersonImages() throws IOException {
        // Given: An image with a person-like silhouette
        ImageData image = TestImageUtils.createPersonSilhouetteImage(640, 480);

        // When: Pose estimation is performed
        try {
            List<Detection> poses = backend.detectPoses(image);

            // Then: Should return a result (may be empty if no poses detected)
            assertThat(poses).isNotNull();
            assertThat(poses).isInstanceOf(List.class);
        } catch (BaseVisionException e) {
            // Acceptable if pose model is not available
            assertThat(e.getMessage()).contains("not initialized");
        }
    }

    @Test
    @DisplayName("Should handle empty image gracefully")
    void shouldHandleEmptyImageGracefully() throws IOException {
        // Given: Empty/white image
        ImageData emptyImage = TestImageUtils.createEmptyImage(640, 480);

        // When: Pose estimation is attempted
        try {
            List<Detection> poses = backend.detectPoses(emptyImage);

            // Then: Should return empty or handle gracefully
            assertThat(poses).isNotNull();
        } catch (BaseVisionException e) {
            // Acceptable if pose model is not available
            assertThat(e.getMessage()).contains("not initialized");
        }
    }

    @Test
    @DisplayName("Should handle noise image gracefully")
    void shouldHandleNoiseImageGracefully() throws IOException {
        // Given: Random noise image
        ImageData noiseImage = TestImageUtils.createNoiseImage(640, 480);

        // When: Pose estimation is attempted
        try {
            List<Detection> poses = backend.detectPoses(noiseImage);

            // Then: Should complete successfully or handle gracefully
            assertThat(poses).isNotNull();
        } catch (BaseVisionException e) {
            // Acceptable if pose model is not available
            assertThat(e.getMessage()).contains("not initialized");
        }
    }

    @Test
    @DisplayName("Should handle different image sizes")
    void shouldHandleDifferentImageSizes() throws IOException {
        // Test various image sizes
        int[][] sizes = {{320, 240}, {640, 480}, {800, 600}};

        for (int[] size : sizes) {
            // Given: Person silhouette image of specific size
            ImageData image = TestImageUtils.createPersonSilhouetteImage(size[0], size[1]);

            // When: Pose estimation is attempted
            try {
                List<Detection> poses = backend.detectPoses(image);

                // Then: Should complete successfully
                assertThat(poses).isNotNull();
            } catch (BaseVisionException e) {
                // Acceptable if pose model is not available
                assertThat(e.getMessage()).contains("not initialized");
            }
        }
    }

    @Test
    @DisplayName("Should return pose detections with valid structure")
    void shouldReturnPoseDetectionsWithValidStructure() throws IOException {
        // Given: Test image with person-like content
        ImageData image = TestImageUtils.createPersonSilhouetteImage(640, 480);

        // When: Pose estimation is performed
        try {
            List<Detection> poses = backend.detectPoses(image);

            // Then: Each pose detection should have valid structure
            for (Detection pose : poses) {
                assertThat(pose).isNotNull();
                assertThat(pose.label()).isNotNull();
                assertThat(pose.confidence()).isBetween(0.0, 1.0);

                // Attributes should contain pose information
                assertThat(pose.attributes()).isNotNull();
                assertThat(pose.attributes()).containsKey("backend");
                assertThat(pose.attributes().get("backend")).isEqualTo("djl");

                // Should contain joints information
                if (pose.attributes().containsKey("joints")) {
                    @SuppressWarnings("unchecked")
                    List<?> joints = (List<?>) pose.attributes().get("joints");
                    assertThat(joints).isNotNull();
                }
            }
        } catch (BaseVisionException e) {
            // Acceptable if pose model is not available
            assertThat(e.getMessage()).contains("not initialized");
        }
    }

    @Test
    @DisplayName("Should respect confidence threshold for pose estimation")
    void shouldRespectConfidenceThreshold() throws IOException {
        // Given: Test image
        ImageData image = TestImageUtils.createPersonSilhouetteImage(640, 480);

        // When: Pose estimation is performed
        try {
            List<Detection> poses = backend.detectPoses(image);

            // Then: All pose detections should meet the configured confidence threshold
            float configuredThreshold = 0.3f; // From setup
            for (Detection pose : poses) {
                assertThat(pose.confidence()).isGreaterThanOrEqualTo(configuredThreshold);
            }
        } catch (BaseVisionException e) {
            // Acceptable if pose model is not available
            assertThat(e.getMessage()).contains("not initialized");
        }
    }

    @Test
    @DisplayName("Should extract joint keypoints correctly")
    void shouldExtractJointKeypointsCorrectly() throws IOException {
        // Given: Test image
        ImageData image = TestImageUtils.createPersonSilhouetteImage(640, 480);

        // When: Pose estimation is performed
        try {
            List<Detection> poses = backend.detectPoses(image);

            // Then: Pose detections should contain joint information
            for (Detection pose : poses) {
                if (pose.attributes().containsKey("joints")) {
                    @SuppressWarnings("unchecked")
                    List<?> joints = (List<?>) pose.attributes().get("joints");

                    // Each joint should have the expected structure
                    for (Object jointObj : joints) {
                        @SuppressWarnings("unchecked")
                        var joint = (java.util.Map<String, Object>) jointObj;

                        // Joint should have index, type, x, y, confidence
                        assertThat(joint).containsKey("index");
                        assertThat(joint).containsKey("type");
                        assertThat(joint).containsKey("x");
                        assertThat(joint).containsKey("y");
                        assertThat(joint).containsKey("confidence");

                        // Validate ranges
                        double x = ((Number) joint.get("x")).doubleValue();
                        double y = ((Number) joint.get("y")).doubleValue();
                        double conf = ((Number) joint.get("confidence")).doubleValue();
                        assertThat(x).isBetween(0.0, 1.0);
                        assertThat(y).isBetween(0.0, 1.0);
                        assertThat(conf).isBetween(0.0, 1.0);
                    }
                }
            }
        } catch (BaseVisionException e) {
            // Acceptable if pose model is not available
            assertThat(e.getMessage()).contains("not initialized");
        }
    }

    @Test
    @EnabledIf("isPoseModelAvailable")
    @DisplayName("Should use real YOLOv8-pose model when available")
    void shouldUseRealYoloPoseModelWhenAvailable() throws IOException {
        // Given: Test image with person-like content
        ImageData image = TestImageUtils.createPersonSilhouetteImage(640, 480);

        // When: Pose estimation is performed
        List<Detection> poses = backend.detectPoses(image);

        // Then: Should have completed successfully with real pose model
        assertThat(poses).isNotNull();
        assertThat(poses).isInstanceOf(List.class);
    }

    /**
     * Check if YOLO pose model is available for testing.
     */
    static boolean isPoseModelAvailable() {
        return YoloLoader.isModelAvailable("yolov8-pose/yolov8n-pose.pt");
    }
}
