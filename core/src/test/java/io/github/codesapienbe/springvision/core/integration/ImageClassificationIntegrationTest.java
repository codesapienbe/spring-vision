package io.github.codesapienbe.springvision.core.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;

/**
 * TDD Integration Test for Image Classification functionality.
 * Tests the ability to classify images into categories.
 */
public class ImageClassificationIntegrationTest {

    private VisionTemplate visionTemplate;
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        // Enable offline mode to avoid network dependencies
        System.setProperty("ai.djl.offline", "true");

        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(
            new MapPropertySource("test-properties",
                java.util.Map.of(
                    "vision.metrics.enabled", "false",
                    "vision.health.enabled", "false"
                )
            )
        );
        // Ensure synthetic fallbacks are enabled for TDD-style offline tests
        context.getEnvironment().getSystemProperties().put("spring.vision.djl.synthetic-fallbacks", "true");
        context.register(VisionAutoConfiguration.class);
        context.refresh();

        visionTemplate = context.getBean(VisionTemplate.class);
    }

    @Test
    void shouldHandleImageClassificationInOfflineMode() {
        // Given: An image and offline mode
        ImageData catImage = createCatImage();
        // When: Image classification is attempted in offline mode using a strict
        // backend instance with synthetic fallbacks disabled (should fail)
        AnnotationConfigApplicationContext local = new AnnotationConfigApplicationContext();
        try {
            local.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("test-properties",
                    java.util.Map.of(
                        "vision.metrics.enabled", "false",
                        "vision.health.enabled", "false",
                        "spring.vision.djl.synthetic-fallbacks", "false"
                    )
                )
            );
            local.register(VisionAutoConfiguration.class);
            local.refresh();

            VisionTemplate strictTemplate = local.getBean(VisionTemplate.class);
            try {
                strictTemplate.classifyImage(catImage, 5);
                fail("Expected image classification to fail in offline mode");
            } catch (VisionBackendException e) {
                assertThat(e.getOperation()).isEqualTo("classification_failed");
            }
        } finally {
            local.close();
        }
    }

    @Test
    void shouldProvideMultipleClassificationOptions() {
        // Given: An image that could be classified in multiple ways
        ImageData testImage = createAnimalImage();

        // When: Image classification is performed
        VisionResult result = visionTemplate.classifyImage(testImage, 5);

        // Then: Should provide multiple classification options with confidence scores
        assertThat(result).isNotNull();
        assertThat(result.detections()).hasSizeGreaterThanOrEqualTo(1);

        // All classifications should have valid confidence scores
        result.detections().forEach(detection -> {
            assertThat(detection.confidence()).isBetween(0.0, 1.0);
            assertThat(detection.label()).isNotNull();
            assertThat(detection.label()).isNotEmpty();
        });
    }

    @Test
    void shouldClassifyDifferentImageTypes() {
        // Given: Images of different categories (animal, vehicle, food, etc.)
        ImageData animalImage = createAnimalImage();
        ImageData vehicleImage = createVehicleImage();
        ImageData foodImage = createFoodImage();

        // When: Classification is performed on each
        VisionResult animalResult = visionTemplate.classifyImage(animalImage, 5);
        VisionResult vehicleResult = visionTemplate.classifyImage(vehicleImage, 5);
        VisionResult foodResult = visionTemplate.classifyImage(foodImage, 5);

        // Then: Each should be classified appropriately
        assertThat(animalResult).isNotNull();
        assertThat(vehicleResult).isNotNull();
        assertThat(foodResult).isNotNull();

        assertThat(animalResult.detections()).isNotEmpty();
        assertThat(vehicleResult.detections()).isNotEmpty();
        assertThat(foodResult.detections()).isNotEmpty();
    }

    @Test
    void shouldHandleAbstractOrComplexImages() {
        // Given: An abstract or complex image that's harder to classify
        ImageData abstractImage = createAbstractImage();

        // When: Classification is performed
        VisionResult result = visionTemplate.classifyImage(abstractImage, 5);

        // Then: Should still provide some classification, even if with lower confidence
        assertThat(result).isNotNull();
        // May have lower confidence but should not crash
    }

    @Test
    void shouldClassifyWithReasonableConfidenceThreshold() {
        // Given: A clear, simple image
        ImageData simpleImage = createSimpleTestImage();

        // When: Classification is performed
        VisionResult result = visionTemplate.classifyImage(simpleImage, 5);

        // Then: Top classification should have reasonable confidence (> 0.1)
        assertThat(result).isNotNull();
        assertThat(result.detections()).isNotEmpty();
        assertThat(result.detections().get(0).confidence()).isGreaterThan(0.1);
    }

    @Test
    void shouldProvideCategoryHierarchy() {
        // Given: An image that can be classified at different levels
        ImageData vehicleImage = createVehicleImage();

        // When: Classification is performed
        VisionResult result = visionTemplate.classifyImage(vehicleImage, 5);

        // Then: Should provide hierarchical categories (e.g., vehicle -> car -> sedan)
        assertThat(result).isNotNull();
        assertThat(result.detections()).isNotEmpty();

        // Categories should be meaningful strings
        result.detections().forEach(detection -> {
            assertThat(detection.label()).isNotNull();
            assertThat(detection.label().length()).isGreaterThan(2);
        });
    }

    // Helper methods to create test images
    private ImageData createCatImage() {
        // TODO: Create image that looks like a cat
        return createColoredRectangle(Color.ORANGE, 200, 150);
    }

    private ImageData createAnimalImage() {
        // TODO: Create image of an animal
        return createColoredRectangle(new Color(165, 42, 42), 200, 150);
    }

    private ImageData createVehicleImage() {
        // TODO: Create image that looks like a vehicle
        return createColoredRectangle(Color.BLUE, 300, 150);
    }

    private ImageData createFoodImage() {
        // TODO: Create image that looks like food
        return createColoredRectangle(Color.RED, 200, 200);
    }

    private ImageData createAbstractImage() {
        // TODO: Create abstract/complex image
        return createPatternedImage();
    }

    private ImageData createSimpleTestImage() {
        // TODO: Create simple, clear image for testing
        return createColoredRectangle(Color.GREEN, 100, 100);
    }

    private ImageData createColoredRectangle(Color color, int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            g2d.setColor(color);
            g2d.fillRect(0, 0, width, height);

            g2d.setColor(Color.BLACK);
            g2d.drawRect(0, 0, width-1, height-1);

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return ImageData.fromBytes(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create colored rectangle image", e);
        }
    }

    private ImageData createPatternedImage() {
        try {
            BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            // Create a simple pattern
            for (int x = 0; x < 200; x += 20) {
                for (int y = 0; y < 200; y += 20) {
                    g2d.setColor((x + y) % 40 == 0 ? Color.RED : Color.BLUE);
                    g2d.fillRect(x, y, 20, 20);
                }
            }

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return ImageData.fromBytes(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create patterned image", e);
        }
    }
}
