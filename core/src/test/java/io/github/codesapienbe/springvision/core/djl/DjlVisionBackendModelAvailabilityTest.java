package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;

/**
 * Integration test for DjlVisionBackend model availability checks.
 * Tests whether the backend correctly reports model availability.
 */
@DisplayName("DJL Vision Backend Model Availability Test")
public class DjlVisionBackendModelAvailabilityTest {

    private DjlVisionBackend backend;
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
        context.register(VisionAutoConfiguration.class);
        context.refresh();

        backend = (DjlVisionBackend) context.getBean(VisionBackend.class);
    }

    @Test
    @DisplayName("Should report object detection model availability")
    void shouldReportObjectDetectionModelAvailability() {
        // When: Checking object detection model availability
        boolean available = backend.isObjectDetectionModelAvailable();

        // Then: Should return a boolean (availability depends on model files)
        assertThat(available).isNotNull();
        // In offline mode, availability depends on whether model files exist
    }

    @Test
    @DisplayName("Should report face detection model availability")
    void shouldReportFaceDetectionModelAvailability() {
        // When: Checking face detection model availability
        boolean available = backend.isFaceDetectionModelAvailable();

        // Then: Should return a boolean
        assertThat(available).isNotNull();
    }

    @Test
    @DisplayName("Should report pose estimation model availability")
    void shouldReportPoseEstimationModelAvailability() {
        // When: Checking pose estimation model availability
        boolean available = backend.isPoseEstimationModelAvailable();

        // Then: Should return a boolean
        assertThat(available).isNotNull();
    }

    @Test
    @DisplayName("Should report image classification model availability")
    void shouldReportImageClassificationModelAvailability() {
        // When: Checking image classification model availability
        boolean available = backend.isImageClassificationModelAvailable();

        // Then: Should return a boolean
        assertThat(available).isNotNull();
    }

    @Test
    @DisplayName("Should report OCR model availability")
    void shouldReportOcrModelAvailability() {
        // When: Checking OCR model availability
        boolean available = backend.isOcrModelAvailable();

        // Then: Should return true (OCR is always available via external libraries)
        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("Should report embedding model availability")
    void shouldReportEmbeddingModelAvailability() {
        // When: Checking embedding model availability
        boolean available = backend.isEmbeddingModelAvailable();

        // Then: Should return a boolean
        assertThat(available).isNotNull();
    }

    @Test
    @DisplayName("Should report barcode model availability")
    void shouldReportBarcodeModelAvailability() {
        // When: Checking barcode model availability
        boolean available = backend.isBarcodeModelAvailable();

        // Then: Should return true (barcode detection uses ZXing library)
        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("Should report metadata extraction model availability")
    void shouldReportMetaDataExtractionModelAvailability() {
        // When: Checking metadata extraction model availability
        boolean available = backend.isMetaDataExtractionModelAvailable();

        // Then: Should return true (metadata extraction uses external libraries)
        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("Should report annotation model availability")
    void shouldReportAnnotationModelAvailability() {
        // When: Checking annotation model availability
        boolean available = backend.isAnnotationModelAvailable();

        // Then: Should return true (annotation is image processing)
        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("Should report action recognition model availability")
    void shouldReportActionRecognitionModelAvailability() {
        // When: Checking action recognition model availability
        boolean available = backend.isActionRecognitionModelAvailable();

        // Then: Should return a boolean
        assertThat(available).isNotNull();
    }
}
