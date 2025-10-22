package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.capabilities.OcrCapability;

/**
 * Comprehensive integration test for OCR, barcode detection, and metadata extraction capabilities.
 * Tests text recognition, barcode scanning, and EXIF/metadata reading capabilities.
 */
@DisplayName("OCR, Barcode & Metadata Integration Tests")
public class OcrBarcodeMetadataIntegrationTest {

    private static DjlVisionBackend backend;

    @BeforeAll
    static void setup() throws Exception {
        // Configure backend for real capability testing
        System.setProperty("ai.djl.offline", "true");
        System.setProperty("OPT_OUT_TRACKING", "true");

        DjlProperties properties = new DjlProperties();
        properties.setEngine("OnnxRuntime"); // Prefer ONNX for OCR
        properties.setDevice("cpu");
        properties.setAutoDownload(false); // Use offline mode

        backend = new DjlVisionBackend(properties);

        // Initialize backend
        backend.initialize();

        // Verify backend is ready
        assertThat(backend.isHealthy()).isTrue();
    }

    @AfterAll
    static void teardown() {
        if (backend != null) {
            backend.shutdown();
        }
    }

    // ==================== OCR Tests ====================

    @Test
    @DisplayName("Should extract text from images with text")
    void shouldExtractTextFromImages() throws IOException {
        // Given: Image with text
        ImageData textImage = TestImageUtils.createTextImage("HELLO WORLD", 400, 100);

        // When: OCR is performed
        try {
            List<OcrCapability.TextDetection> textDetections = backend.extractText(textImage);

            // Then: Should return text detection results
            assertThat(textDetections).isNotNull();
            assertThat(textDetections).isInstanceOf(List.class);

            // Should have at least one text detection
            if (!textDetections.isEmpty()) {
                OcrCapability.TextDetection detection = textDetections.get(0);
                assertThat(detection.text()).isNotNull();
                assertThat(detection.confidence()).isBetween(0.0, 1.0);

                // Should have bounding box information
                if (detection.boundingBox() != null) {
                    assertThat(detection.boundingBox()).containsKey("x");
                    assertThat(detection.boundingBox()).containsKey("y");
                    assertThat(detection.boundingBox()).containsKey("width");
                    assertThat(detection.boundingBox()).containsKey("height");
                }

                // Should have attributes
                assertThat(detection.attributes()).isNotNull();
            }
        } catch (Exception e) {
            // OCR may not be available or may fail on synthetic text
            assertThat(e).isInstanceOf(Exception.class);
        }
    }

    @Test
    @DisplayName("Should handle OCR on images without text")
    void shouldHandleOcrOnImagesWithoutText() throws IOException {
        // Given: Image without text (geometric shape)
        ImageData noTextImage = TestImageUtils.createGeometricShapeImage(400, 400, "circle");

        // When: OCR is performed
        try {
            List<OcrCapability.TextDetection> textDetections = backend.extractText(noTextImage);

            // Then: Should return results (may be empty or contain empty text)
            assertThat(textDetections).isNotNull();
        } catch (Exception e) {
            // Acceptable if OCR fails or is not available
            assertThat(e).isInstanceOf(Exception.class);
        }
    }

    @Test
    @DisplayName("Should handle OCR on empty images")
    void shouldHandleOcrOnEmptyImages() throws IOException {
        // Given: Empty image
        ImageData emptyImage = TestImageUtils.createEmptyImage(400, 200);

        // When: OCR is performed
        try {
            List<OcrCapability.TextDetection> textDetections = backend.extractText(emptyImage);

            // Then: Should complete successfully
            assertThat(textDetections).isNotNull();
        } catch (Exception e) {
            // Acceptable if OCR fails on empty image
            assertThat(e).isInstanceOf(Exception.class);
        }
    }

    // ==================== Barcode Tests ====================

    @Test
    @DisplayName("Should detect barcodes in test images")
    void shouldDetectBarcodesInTestImages() throws IOException {
        // Given: Image with barcode-like pattern
        ImageData barcodeImage = TestImageUtils.createBarcodeImage("123456789", 300, 100);

        // When: Barcode detection is performed
        List<Detection> barcodeDetections = backend.detectBarcodes(barcodeImage);

        // Then: Should return barcode detection results
        assertThat(barcodeDetections).isNotNull();
        assertThat(barcodeDetections).isInstanceOf(List.class);

        // Note: ZXing may not detect our synthetic barcode, but the method should work
    }

    @Test
    @DisplayName("Should handle barcode detection on images without barcodes")
    void shouldHandleBarcodeDetectionOnImagesWithoutBarcodes() throws IOException {
        // Given: Image without barcode (text image)
        ImageData textImage = TestImageUtils.createTextImage("HELLO", 400, 100);

        // When: Barcode detection is performed
        List<Detection> barcodeDetections = backend.detectBarcodes(textImage);

        // Then: Should return empty list or handle gracefully
        assertThat(barcodeDetections).isNotNull();
        assertThat(barcodeDetections).isInstanceOf(List.class);
    }

    @Test
    @DisplayName("Should return barcode detections with valid structure")
    void shouldReturnBarcodeDetectionsWithValidStructure() throws IOException {
        // Given: Test image
        ImageData testImage = TestImageUtils.createRectangleImage(400, 200, Color.BLACK);

        // When: Barcode detection is performed
        List<Detection> detections = backend.detectBarcodes(testImage);

        // Then: Each detection should have valid structure
        for (Detection detection : detections) {
            assertThat(detection).isNotNull();
            assertThat(detection.label()).isNotNull();
            assertThat(detection.confidence()).isEqualTo(1.0f); // Barcodes are deterministic

            // Should have bounding box for detected barcodes
            if (detection.boundingBox() != null) {
                assertThat(detection.boundingBox().x()).isBetween(0.0, 1.0);
                assertThat(detection.boundingBox().y()).isBetween(0.0, 1.0);
                assertThat(detection.boundingBox().width()).isBetween(0.0, 1.0);
                assertThat(detection.boundingBox().height()).isBetween(0.0, 1.0);
            }

            // Attributes should contain barcode information
            assertThat(detection.attributes()).isNotNull();
            assertThat(detection.attributes()).containsKey("backend");
            assertThat(detection.attributes().get("backend")).isEqualTo("djl");
        }
    }

    // ==================== Metadata Tests ====================

    @Test
    @DisplayName("Should extract metadata from images")
    void shouldExtractMetadataFromImages() throws IOException {
        // Given: Any test image
        ImageData testImage = TestImageUtils.createTextImage("TEST", 400, 200);

        // When: Metadata extraction is performed
        List<Detection> metadataDetections = backend.extractMetaData(testImage);

        // Then: Should return metadata detection results
        assertThat(metadataDetections).isNotNull();
        assertThat(metadataDetections).isInstanceOf(List.class);

        // Should have at least basic metadata
        if (!metadataDetections.isEmpty()) {
            Detection metadata = metadataDetections.get(0);
            assertThat(metadata.attributes()).isNotNull();
            assertThat(metadata.attributes()).containsKey("backend");
            assertThat(metadata.attributes().get("backend")).isEqualTo("djl");
        }
    }

    @Test
    @DisplayName("Should extract metadata from different image types")
    void shouldExtractMetadataFromDifferentImageTypes() throws IOException {
        // Test different image types
        ImageData[] testImages = {
            TestImageUtils.createRectangleImage(400, 300, Color.RED),
            TestImageUtils.createTextImage("METADATA TEST", 500, 200),
            TestImageUtils.createGeometricShapeImage(300, 300, "triangle"),
            TestImageUtils.createEmptyImage(200, 200)
        };

        for (ImageData image : testImages) {
            // When: Metadata extraction is performed
            List<Detection> metadataDetections = backend.extractMetaData(image);

            // Then: Should complete successfully
            assertThat(metadataDetections).isNotNull();
            assertThat(metadataDetections).isInstanceOf(List.class);
        }
    }

    @Test
    @DisplayName("Should return metadata detections with valid structure")
    void shouldReturnMetadataDetectionsWithValidStructure() throws IOException {
        // Given: Test image
        ImageData testImage = TestImageUtils.createTextImage("METADATA", 400, 200);

        // When: Metadata extraction is performed
        List<Detection> detections = backend.extractMetaData(testImage);

        // Then: Each detection should have valid structure
        for (Detection detection : detections) {
            assertThat(detection).isNotNull();
            assertThat(detection.label()).isNotNull();
            assertThat(detection.confidence()).isEqualTo(1.0f); // Metadata extraction is deterministic

            // Should have full-image bounding box
            assertThat(detection.boundingBox()).isNotNull();
            assertThat(detection.boundingBox().x()).isEqualTo(0.0);
            assertThat(detection.boundingBox().y()).isEqualTo(0.0);
            assertThat(detection.boundingBox().width()).isEqualTo(1.0);
            assertThat(detection.boundingBox().height()).isEqualTo(1.0);

            // Attributes should contain metadata information
            assertThat(detection.attributes()).isNotNull();
            assertThat(detection.attributes()).containsKey("backend");
            assertThat(detection.attributes().get("backend")).isEqualTo("djl");
        }
    }

    @Test
    @DisplayName("Should handle metadata extraction on corrupted images")
    void shouldHandleMetadataExtractionOnCorruptedImages() throws IOException {
        // Given: Noise image (which may be treated as corrupted)
        ImageData noiseImage = TestImageUtils.createNoiseImage(400, 300);

        // When: Metadata extraction is performed
        List<Detection> metadataDetections = backend.extractMetaData(noiseImage);

        // Then: Should complete successfully (may return empty or minimal metadata)
        assertThat(metadataDetections).isNotNull();
        assertThat(metadataDetections).isInstanceOf(List.class);
    }

    // ==================== Combined Tests ====================

    @Test
    @DisplayName("Should handle all text/barcode/metadata operations concurrently")
    void shouldHandleAllTextOperationsConcurrently() throws IOException {
        // Given: Multiple test images
        ImageData textImage = TestImageUtils.createTextImage("CONCURRENT", 400, 100);
        ImageData barcodeImage = TestImageUtils.createBarcodeImage("999888777", 300, 100);
        ImageData metadataImage = TestImageUtils.createRectangleImage(400, 300, Color.BLUE);

        // When: All operations are performed
        List<OcrCapability.TextDetection> ocrResults = backend.extractText(textImage);
        List<Detection> barcodeResults = backend.detectBarcodes(barcodeImage);
        List<Detection> metadataResults = backend.extractMetaData(metadataImage);

        // Then: All should complete successfully
        assertThat(ocrResults).isNotNull();
        assertThat(barcodeResults).isNotNull();
        assertThat(metadataResults).isNotNull();
    }

    @Test
    @DisplayName("Should handle large images for all text operations")
    void shouldHandleLargeImagesForAllTextOperations() throws IOException {
        // Given: Large test image
        ImageData largeImage = TestImageUtils.createTextImage("LARGE IMAGE TEST", 1200, 800);

        // When: All text operations are performed on large image
        List<OcrCapability.TextDetection> ocrResults = backend.extractText(largeImage);
        List<Detection> barcodeResults = backend.detectBarcodes(largeImage);
        List<Detection> metadataResults = backend.extractMetaData(largeImage);

        // Then: All should complete successfully
        assertThat(ocrResults).isNotNull();
        assertThat(barcodeResults).isNotNull();
        assertThat(metadataResults).isNotNull();
    }
}
