package io.github.codesapienbe.springvision.core.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Central Integration Test for all Vision capabilities.
 * Tests that models are loaded and detection succeeds, and that invalid input fails loudly.
 */
public class VisionIntegrationTest {

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

    @Nested
    class ObjectDetectionTests {

        @Test
        void shouldSucceedWhenModelsPresent() {
            ImageData image = createPlaceholderImage(640, 480);
            VisionResult result = visionTemplate.detectObjects(image);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    class FaceDetectionTests {

        @Test
        void shouldSucceedWhenModelsPresent() {
            ImageData image = createPlaceholderImage(640, 480);
            VisionResult result = visionTemplate.detectFaces(image);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    class ImageClassificationTests {

        @Test
        void shouldFailWhenModelsUnavailable() {
            ImageData image = createPlaceholderImage(640, 480);
            assertThrows(BaseVisionException.class, () -> visionTemplate.classifyImage(image, 5));
        }
    }

    @Nested
    class OcrTests {

        @Test
        void shouldFailWhenModelsUnavailable() {
            ImageData image = createPlaceholderImage(640, 480);
            assertThrows(BaseVisionException.class, () -> visionTemplate.extractText(image));
        }
    }

    @Nested
    class PoseEstimationTests {

        @Test
        void shouldFailWhenModelsUnavailable() {
            ImageData image = createPlaceholderImage(640, 480);
            assertThrows(BaseVisionException.class, () -> visionTemplate.detectPoses(image));
        }
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
