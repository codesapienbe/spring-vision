package io.github.codesapienbe.springvision.core.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;

/**
 * Central Integration Test for all Vision capabilities.
 * Uses nested classes for each capability to avoid duplication of bean setup.
 */
@SpringBootTest(classes = VisionAutoConfiguration.class)
public class VisionIntegrationTest {

    private VisionTemplate visionTemplate;
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        // Enable offline mode and synthetic fallbacks to avoid network dependencies
        System.setProperty("ai.djl.offline", "true");

        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(
            new MapPropertySource("test-properties",
                java.util.Map.of(
                    "vision.metrics.enabled", "false",
                    "vision.health.enabled", "false",
                    "vision.djl.syntheticFallbacks", "true"
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
        void shouldDetectCommonObjectsInImage() {
            // Given: An image containing common objects (person, car, chair, etc.)
            ImageData imageWithObjects = createImageWithCommonObjects();

            // When: Object detection is performed
            VisionResult result = visionTemplate.detectObjects(imageWithObjects);

            // Then: Multiple objects should be detected
            assertThat(result).isNotNull();
            assertThat(result.detectionCount()).isGreaterThanOrEqualTo(3);
            assertThat(result.averageConfidence()).isGreaterThan(0.5);
        }

        @Test
        void shouldDetectSpecificObjectTypes() {
            // Given: An image with vehicles
            ImageData vehicleImage = createImageWithVehicles();

            // When: Object detection is performed
            VisionResult result = visionTemplate.detectObjects(vehicleImage);

            // Then: Should detect cars, trucks, or other vehicles
            assertThat(result).isNotNull();
            assertThat(result.detections()).anyMatch(detection ->
                detection.label().toLowerCase().contains("car") ||
                    detection.label().toLowerCase().contains("truck") ||
                    detection.label().toLowerCase().contains("vehicle")
            );
        }

        @Test
        void shouldDetectPeopleAsObjects() {
            // Given: An image with people
            ImageData peopleImage = createImageWithPeople();

            // When: Object detection is performed
            VisionResult result = visionTemplate.detectObjects(peopleImage);

            // Then: Should detect person objects
            assertThat(result).isNotNull();
            assertThat(result.detections()).anyMatch(detection ->
                detection.label().toLowerCase().contains("person")
            );
        }

        @Test
        void shouldProvideConfidenceScoresForDetections() {
            // Given: An image with detectable objects
            ImageData testImage = createImageWithCommonObjects();

            // When: Object detection is performed
            VisionResult result = visionTemplate.detectObjects(testImage);

            // Then: All detections should have confidence scores between 0 and 1
            assertThat(result).isNotNull();
            assertThat(result.detections()).isNotEmpty();

            result.detections().forEach(detection -> {
                assertThat(detection.confidence()).isBetween(0.0, 1.0);
            });
        }

        @Test
        void shouldDetectObjectsWithBoundingBoxes() {
            // Given: An image with objects
            ImageData objectImage = createImageWithCommonObjects();

            // When: Object detection is performed
            VisionResult result = visionTemplate.detectObjects(objectImage);

            // Then: Each detection should have valid bounding box coordinates
            assertThat(result).isNotNull();
            assertThat(result.detections()).isNotEmpty();

            result.detections().forEach(detection -> {
                assertThat(detection.boundingBox()).isNotNull();
                assertThat(detection.boundingBox().x()).isGreaterThanOrEqualTo(0);
                assertThat(detection.boundingBox().y()).isGreaterThanOrEqualTo(0);
                assertThat(detection.boundingBox().width()).isGreaterThan(0);
                assertThat(detection.boundingBox().height()).isGreaterThan(0);
            });
        }

        @Test
        void shouldHandleEmptyOrMinimalImages() {
            // Given: An image with very few or no detectable objects
            ImageData minimalImage = createMinimalImage();

            // When: Object detection is performed
            VisionResult result = visionTemplate.detectObjects(minimalImage);

            // Then: Should handle gracefully (may return empty results)
            assertThat(result).isNotNull();
            // Result may be empty, but should not throw exceptions
        }
    }

    @Nested
    class FaceDetectionTests {

        @Test
        void shouldDetectFacesInImage() {
            // Given: An image with faces
            ImageData faceImage = createImageWithFaces();

            // When: Face detection is performed
            VisionResult result = visionTemplate.detectFaces(faceImage);

            // Then: Faces should be detected
            assertThat(result).isNotNull();
            assertThat(result.detectionCount()).isGreaterThanOrEqualTo(1);
        }

        // Add more face detection tests here
    }

    @Nested
    class ImageClassificationTests {

        @Test
        void shouldClassifyImage() {
            // Given: An image to classify
            ImageData image = createImageForClassification();

            // When: Image classification is performed
            VisionResult result = visionTemplate.classifyImage(image, 5);

            // Then: Classifications should be returned
            assertThat(result).isNotNull();
            assertThat(result.detections()).isNotEmpty();
        }

        // Add more classification tests here
    }

    @Nested
    class OcrTests {

        @Test
        void shouldExtractTextFromImage() {
            // Given: An image with text
            ImageData textImage = createImageWithText();

            // When: OCR is performed
            VisionResult result = visionTemplate.extractText(textImage);

            // Then: Text should be extracted
            assertThat(result).isNotNull();
            assertThat(result.detections()).isNotEmpty();
        }

        // Add more OCR tests here
    }

    @Nested
    class PoseEstimationTests {

        @Test
        void shouldDetectPosesInImage() {
            // Given: An image with people for pose estimation
            ImageData poseImage = createImageWithPeople();

            // When: Pose detection is performed
            VisionResult result = visionTemplate.detectPoses(poseImage);

            // Then: Poses should be detected
            assertThat(result).isNotNull();
            // Add assertions based on expected results
        }

        // Add more pose estimation tests here
    }

    // Helper methods to create test images
    private ImageData createImageWithCommonObjects() {
        // TODO: Create image with chairs, tables, people, etc.
        return createPlaceholderImage(640, 480);
    }

    private ImageData createImageWithVehicles() {
        // TODO: Create image with cars, trucks, etc.
        return createPlaceholderImage(640, 480);
    }

    private ImageData createImageWithPeople() {
        // TODO: Create image with multiple people
        return createPlaceholderImage(640, 480);
    }

    private ImageData createImageWithFaces() {
        // TODO: Create image with faces
        return createPlaceholderImage(640, 480);
    }

    private ImageData createImageForClassification() {
        // TODO: Create image for classification
        return createPlaceholderImage(640, 480);
    }

    private ImageData createImageWithText() {
        // TODO: Create image with text
        return createPlaceholderImage(640, 480);
    }

    private ImageData createMinimalImage() {
        // TODO: Create image with minimal content
        return createPlaceholderImage(100, 100);
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
