package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;

/**
 * Integration test for DjlVisionBackend initialization and basic functionality.
 * Tests backend health, model loading, and basic inference capabilities.
 */
@DisplayName("DJL Vision Backend Integration Test")
public class DjlVisionBackendIntegrationTest {

    private DjlVisionBackend backend;

    @BeforeEach
    void setUp() {
        // Enable offline mode to avoid network dependencies
        System.setProperty("ai.djl.offline", "true");

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(
            new MapPropertySource("test-properties",
                java.util.Map.of(
                    "vision.metrics.enabled", "false",
                    "vision.health.enabled", "false"
                )
            )
        );
        context.register(VisionAutoConfiguration.class);
        context.refresh();

        backend = (DjlVisionBackend) context.getBean(VisionBackend.class);
    }

    @Test
    @DisplayName("Should initialize DJL backend successfully")
    void shouldInitializeBackendSuccessfully() {
        // When: Backend is initialized
        backend.initialize();

        // Then: Backend should be healthy and initialized
        assertThat(backend.isHealthy()).isTrue();
        assertThat(backend.getBackendId()).isEqualTo("djl");
        assertThat(backend.getDisplayName()).isEqualTo("DJL Vision Backend");
        assertThat(backend.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("Should provide backend health information")
    void shouldProvideBackendHealthInformation() {
        // When: Getting health info
        var healthInfo = backend.getHealthInfo();

        // Then: Health info should contain expected details
        assertThat(healthInfo).isNotNull();
        assertThat(healthInfo.status()).isNotNull();
        assertThat(healthInfo.metrics()).isNotNull();
        assertThat(healthInfo.metrics().get("engine")).isNotNull();
        assertThat(healthInfo.metrics().get("device")).isNotNull();
    }

    @Test
    @DisplayName("Should support expected detection types")
    void shouldSupportExpectedDetectionTypes() {
        // When: Getting supported detection types
        var supportedTypes = backend.getSupportedDetectionTypes();

        // Then: Should include basic types
        assertThat(supportedTypes).isNotNull();
        assertThat(supportedTypes).containsAnyOf(
            io.github.codesapienbe.springvision.core.DetectionType.FACE,
            io.github.codesapienbe.springvision.core.DetectionType.OBJECT
        );
    }

    @Test
    @DisplayName("Should handle basic object detection")
    void shouldHandleBasicObjectDetection() {
        // Given: A simple test image
        ImageData testImage = createTestImage();

        // When: Performing object detection
        var detections = backend.detectObjects(testImage);

        // Then: Should return detections (may be synthetic in offline mode)
        assertThat(detections).isNotNull();
        // In offline mode, may return empty or synthetic results
    }

    @Test
    @DisplayName("Should handle basic face detection")
    void shouldHandleBasicFaceDetection() {
        // Given: A simple test image
        ImageData testImage = createTestImage();

        // When: Performing face detection
        var detections = backend.detectFaces(testImage);

        // Then: Should return detections (may be synthetic in offline mode)
        assertThat(detections).isNotNull();
        // In offline mode, may return empty or synthetic results
    }

    @Test
    @DisplayName("Should handle OCR extraction")
    void shouldHandleOcrExtraction() {
        // Given: A simple test image
        ImageData testImage = createTestImage();

        // When: Performing OCR
        var textDetections = backend.extractText(testImage);

        // Then: Should return text detections (may be synthetic in offline mode)
        assertThat(textDetections).isNotNull();
        // In offline mode, may return synthetic results
    }

    @Test
    @DisplayName("Should handle image classification")
    void shouldHandleImageClassification() {
        // Given: A simple test image
        ImageData testImage = createTestImage();

        // When: Performing image classification
        var classifications = backend.classifyImage(testImage, 5);

        // Then: Should return classifications (may be synthetic in offline mode)
        assertThat(classifications).isNotNull();
        // In offline mode, may return synthetic results
    }

    private ImageData createTestImage() {
        try {
            BufferedImage image = new BufferedImage(224, 224, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return ImageData.fromBytes(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test image", e);
        }
    }
}
