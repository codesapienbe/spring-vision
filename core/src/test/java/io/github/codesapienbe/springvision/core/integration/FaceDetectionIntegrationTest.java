package io.github.codesapienbe.springvision.core.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;

/**
 * TDD Integration Test for Face Detection functionality.
 * This test defines the expected behavior and will initially fail until implementation is complete.
 */
public class FaceDetectionIntegrationTest {

    private VisionTemplate visionTemplate;
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        // Enable offline mode to avoid network dependencies
        System.setProperty("ai.djl.offline", "true");
        // Ensure synthetic fallbacks are explicitly disabled at JVM level for strict offline tests
        System.setProperty("spring.vision.djl.synthetic-fallbacks", "false");

        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(
            new MapPropertySource("test-properties",
                java.util.Map.of(
                    "vision.metrics.enabled", "false",
                    "vision.health.enabled", "false",
                    // Explicitly disable synthetic fallbacks for strict offline face tests
                    "spring.vision.djl.synthetic-fallbacks", "false"
                )
            )
        );
        context.register(VisionAutoConfiguration.class);
        context.refresh();

        visionTemplate = context.getBean(VisionTemplate.class);
    }

    @Test
    void shouldHandleFaceDetectionInOfflineMode() {
        // Given: An image and offline mode (no models loaded)
        ImageData singleFaceImage = createTestImageWithSingleFace();

        // When: Face detection is performed in offline mode
        VisionResult result = visionTemplate.detectFaces(singleFaceImage);

        // Then: Should return empty result gracefully (models not loaded in offline mode)
        assertThat(result).isNotNull();
        assertThat(result.detectionCount()).isEqualTo(0);
        assertThat(result.detections()).isEmpty();
        assertThat(result.detectionType()).isEqualTo(DetectionType.FACE);
    }

    @Test
    void shouldHandleMultipleFaceDetectionInOfflineMode() {
        // Given: An image with multiple faces and offline mode
        ImageData groupPhoto = createTestImageWithMultipleFaces();

        // When: Face detection is performed in offline mode
        VisionResult result = visionTemplate.detectFaces(groupPhoto);

        // Then: Should return empty result gracefully
        assertThat(result).isNotNull();
        assertThat(result.detectionCount()).isEqualTo(0);
        assertThat(result.detections()).isEmpty();
        assertThat(result.detectionType()).isEqualTo(DetectionType.FACE);
    }

    @Test
    void shouldReturnEmptyResultForImageWithoutFacesInOfflineMode() {
        // Given: An image with no faces and offline mode
        ImageData noFaceImage = createTestImageWithoutFaces();

        // When: Face detection is performed in offline mode
        VisionResult result = visionTemplate.detectFaces(noFaceImage);

        // Then: No faces should be detected (expected in offline mode)
        assertThat(result).isNotNull();
        assertThat(result.detectionCount()).isEqualTo(0);
        assertThat(result.detections()).isEmpty();
        assertThat(result.detectionType()).isEqualTo(DetectionType.FACE);
    }

    @Test
    void shouldHandleFaceDetectionWithoutThrowingExceptions() {
        // Given: Any test image
        ImageData testImage = createTestImageWithSingleFace();

        // When: Face detection is performed in offline mode
        VisionResult result = visionTemplate.detectFaces(testImage);

        // Then: Should complete without throwing exceptions
        assertThat(result).isNotNull();
        assertThat(result.detectionType()).isEqualTo(DetectionType.FACE);
    }

    @Test
    void shouldHandleVariousImageFormatsInOfflineMode() {
        // Given: Images in different formats
        ImageData jpegImage = createTestImageJPEG();
        ImageData pngImage = createTestImagePNG();

        // When: Face detection is performed on both in offline mode
        VisionResult jpegResult = visionTemplate.detectFaces(jpegImage);
        VisionResult pngResult = visionTemplate.detectFaces(pngImage);

        // Then: Both should complete without errors (empty results expected in offline mode)
        assertThat(jpegResult).isNotNull();
        assertThat(pngResult).isNotNull();
        assertThat(jpegResult.detectionType()).isEqualTo(DetectionType.FACE);
        assertThat(pngResult.detectionType()).isEqualTo(DetectionType.FACE);
    }

    @Test
    void shouldHandleLargeImagesInOfflineMode() {
        // Given: A high-resolution image
        ImageData largeImage = createLargeTestImage();

        // When: Face detection is performed in offline mode
        VisionResult result = visionTemplate.detectFaces(largeImage);

        // Then: Should complete without throwing exceptions
        assertThat(result).isNotNull();
        assertThat(result.detectionType()).isEqualTo(DetectionType.FACE);
    }

    // Helper methods to create test images - these will need to be implemented
    private ImageData createTestImageWithSingleFace() {
        // TODO: Create or load a test image with exactly one face
        // For now, return a placeholder that will cause the test to fail
        return createPlaceholderImage();
    }

    private ImageData createTestImageWithMultipleFaces() {
        // TODO: Create or load a test image with multiple faces
        return createPlaceholderImage();
    }

    private ImageData createTestImageWithoutFaces() {
        // TODO: Create or load a test image with no faces
        return createPlaceholderImage();
    }

    private ImageData createTestImageJPEG() {
        // TODO: Create JPEG test image
        return createPlaceholderImage();
    }

    private ImageData createTestImagePNG() {
        // TODO: Create PNG test image
        return createPlaceholderImage();
    }

    private ImageData createLargeTestImage() {
        // TODO: Create large resolution test image
        return createPlaceholderImage();
    }

    private ImageData createPlaceholderImage() {
        try {
            // Create a simple 100x100 RGB image as placeholder
            BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return ImageData.fromBytes(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create placeholder image", e);
        }
    }
}
