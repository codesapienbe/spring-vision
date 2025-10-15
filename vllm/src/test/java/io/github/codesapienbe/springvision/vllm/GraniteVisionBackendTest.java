package io.github.codesapienbe.springvision.vllm;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.vllm.config.VllmProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for GraniteVisionBackend.
 * These tests require a running vLLM server with Granite model.
 */
@EnabledIfEnvironmentVariable(named = "VLLM_ENABLED", matches = "true")
class GraniteVisionBackendTest {

    private GraniteVisionBackend backend;

    @BeforeEach
    void setUp() {
        VllmProperties properties = new VllmProperties(
            true,
            "http://localhost:8000",
            "ibm-granite/granite-3.2-8b-instruct",
            Duration.ofSeconds(60),
            512,
            0.7,
            0.9,
            0.7,
            false
        );
        backend = new GraniteVisionBackend(properties);
    }

    @Test
    void testBackendMetadata() {
        assertThat(backend.getBackendId()).isEqualTo("granite-vision");
        assertThat(backend.getDisplayName()).isEqualTo("Granite Vision Backend (vLLM)");
        assertThat(backend.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void testSupportedDetectionTypes() {
        assertThat(backend.getSupportedDetectionTypes())
            .isNotEmpty()
            .contains(
                io.github.codesapienbe.springvision.core.DetectionType.OBJECT,
                io.github.codesapienbe.springvision.core.DetectionType.TEXT
            );
    }

    @Test
    void testHealthCheck() {
        if (backend.isHealthy()) {
            assertThat(backend.getHealthInfo().status())
                .isEqualTo(io.github.codesapienbe.springvision.core.BackendHealthInfo.HealthStatus.HEALTHY);
        }
    }

    @Test
    void testObjectDetection() throws Exception {
        // This test requires a test image and running vLLM server
        byte[] imageBytes = createTestImage();
        ImageData imageData = ImageData.fromBytes(imageBytes);

        List<Detection> detections = backend.detectObjects(imageData);

        assertThat(detections).isNotNull();
        // Add more specific assertions based on test image content
    }

    @Test
    void testTextDetection() throws Exception {
        byte[] imageBytes = createTestImage();
        ImageData imageData = ImageData.fromBytes(imageBytes);

        List<Detection> detections = backend.detectText(imageData);

        assertThat(detections).isNotNull();
    }

    @Test
    void testSceneAnalysis() throws Exception {
        byte[] imageBytes = createTestImage();
        ImageData imageData = ImageData.fromBytes(imageBytes);

        String description = backend.analyzeScene(imageData);

        assertThat(description).isNotNull().isNotEmpty();
    }

    @Test
    void testVisualQuestionAnswering() throws Exception {
        byte[] imageBytes = createTestImage();
        ImageData imageData = ImageData.fromBytes(imageBytes);

        String answer = backend.answerQuestion(imageData, "What do you see in this image?");

        assertThat(answer).isNotNull().isNotEmpty();
    }

    private byte[] createTestImage() {
        // Create a simple test image or load from test resources
        // For now, return empty array - in real tests, load actual image
        return new byte[0];
    }
}
