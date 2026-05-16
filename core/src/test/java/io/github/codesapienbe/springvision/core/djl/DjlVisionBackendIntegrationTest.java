package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Integration test for DjlVisionBackend initialization and basic functionality.
 * Tests backend health, model loading, and basic inference capabilities.
 */
@DisplayName("DJL Vision Backend Integration Test")
public class DjlVisionBackendIntegrationTest {

    private DjlVisionBackend backend;

    @BeforeEach
    void setUp() {
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
        backend.initialize();

        assertThat(backend.isHealthy()).isTrue();
        assertThat(backend.getBackendId()).isEqualTo("djl");
        assertThat(backend.getDisplayName()).isEqualTo("DJL Vision Backend");
        assertThat(backend.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("Should provide backend health information")
    void shouldProvideBackendHealthInformation() {
        var healthInfo = backend.getHealthInfo();

        assertThat(healthInfo).isNotNull();
        assertThat(healthInfo.status()).isNotNull();
        assertThat(healthInfo.metrics()).isNotNull();
        assertThat(healthInfo.metrics().get("engine")).isNotNull();
        assertThat(healthInfo.metrics().get("device")).isNotNull();
    }

    @Test
    @DisplayName("Should support expected detection types")
    void shouldSupportExpectedDetectionTypes() {
        var supportedTypes = backend.getSupportedDetectionTypes();

        assertThat(supportedTypes).isNotNull();
        assertThat(supportedTypes).containsAnyOf(
            io.github.codesapienbe.springvision.core.DetectionType.FACE,
            io.github.codesapienbe.springvision.core.DetectionType.OBJECT
        );
    }

    @Test
    @DisplayName("Should fail object detection when models are unavailable in offline mode")
    void shouldFailObjectDetectionWhenModelsUnavailable() {
        ImageData testImage = createTestImage();
        assertThrows(BaseVisionException.class, () -> backend.detectObjects(testImage));
    }

    @Test
    @DisplayName("Should fail face detection when models are unavailable in offline mode")
    void shouldFailFaceDetectionWhenModelsUnavailable() {
        ImageData testImage = createTestImage();
        assertThrows(BaseVisionException.class, () -> backend.detectFaces(testImage));
    }

    @Test
    @DisplayName("Should fail OCR extraction when models are unavailable in offline mode")
    void shouldFailOcrExtractionWhenModelsUnavailable() {
        ImageData testImage = createTestImage();
        assertThrows(BaseVisionException.class, () -> backend.extractText(testImage));
    }

    @Test
    @DisplayName("Should fail image classification when models are unavailable in offline mode")
    void shouldFailImageClassificationWhenModelsUnavailable() {
        ImageData testImage = createTestImage();
        assertThrows(BaseVisionException.class, () -> backend.classifyImage(testImage, 5));
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
