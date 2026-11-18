package io.github.codesapienbe.springvision.core.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException;

/**
 * Integration test for object detection.
 * Verifies that object detection fails loudly while no licence-clean model is bundled.
 */
public class ObjectDetectionIntegrationTest {

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
    void objectDetectionIsUnsupportedWithoutLicenceCleanModel() {
        ImageData image = createPlaceholderImage(640, 480);
        assertThatThrownBy(() -> visionTemplate.detectObjects(image))
            .isInstanceOf(VisionUnsupportedException.class)
            .hasMessageContaining("licence-clean");
    }

    private ImageData createPlaceholderImage(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return ImageData.fromBytes(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create placeholder image", e);
        }
    }
}
