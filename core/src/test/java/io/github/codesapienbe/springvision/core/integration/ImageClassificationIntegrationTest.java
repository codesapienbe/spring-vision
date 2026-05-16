package io.github.codesapienbe.springvision.core.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;

/**
 * Integration test for image classification.
 * In offline mode (no models loaded), classification must fail with a meaningful exception.
 */
public class ImageClassificationIntegrationTest {

    private VisionTemplate visionTemplate;

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

        visionTemplate = context.getBean(VisionTemplate.class);
    }

    @Test
    void shouldFailWithMeaningfulErrorInOfflineMode() {
        ImageData image = createSimpleTestImage();

        VisionBackendException ex = assertThrows(
            VisionBackendException.class,
            () -> visionTemplate.classifyImage(image, 5));

        assertThat(ex.getOperation()).isEqualTo("classification_failed");
    }

    @Test
    void shouldFailForAnyImageWhenModelsUnavailable() {
        ImageData image = createSimpleTestImage();
        assertThrows(BaseVisionException.class, () -> visionTemplate.classifyImage(image, 5));
    }

    private ImageData createSimpleTestImage() {
        try {
            BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return ImageData.fromBytes(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test image", e);
        }
    }
}
