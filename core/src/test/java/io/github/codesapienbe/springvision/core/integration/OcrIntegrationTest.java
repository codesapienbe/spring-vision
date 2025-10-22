package io.github.codesapienbe.springvision.core.integration;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import static org.junit.jupiter.api.Assertions.fail;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Integration Test for OCR (Optical Character Recognition) functionality.
 * Tests the ability to extract text from images.
 */
public class OcrIntegrationTest {

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
        context.register(VisionAutoConfiguration.class);
        context.refresh();

        visionTemplate = context.getBean(VisionTemplate.class);
    }

    @Test
    void shouldHandleOcrInOfflineMode() {
        // Given: An image with text and offline mode
        ImageData documentImage = createDocumentImageWithText("Hello World");

        // When: OCR is attempted in offline mode
        try {
            visionTemplate.extractText(documentImage);
            // If we reach here, OCR worked (unexpected in offline mode)
            fail("Expected OCR to fail in offline mode");
        } catch (VisionBackendException e) {
            // Then: Should fail gracefully with appropriate error
            assertThat(e.getOperation()).isEqualTo("not_initialized");
        }
    }

    @Test
    void shouldExtractTextFromMultipleLines() {
        // Given: An image with multi-line text
        String expectedText = "Line 1\nLine 2\nLine 3";
        ImageData multiLineImage = createDocumentImageWithText(expectedText);

        // When: OCR is performed
        VisionResult result = visionTemplate.extractText(multiLineImage);

        // Then: All lines should be extracted
        assertThat(result).isNotNull();
        assertThat(result.extractedText()).isNotNull();
        // Allow for some OCR inaccuracy but should contain most words
        assertThat(result.extractedText().toLowerCase()).contains("line");
    }

    @Test
    void shouldHandleDifferentFontsAndSizes() {
        // Given: An image with various font sizes and styles
        ImageData variedFontImage = createImageWithVariedFonts();

        // When: OCR is performed
        VisionResult result = visionTemplate.extractText(variedFontImage);

        // Then: Should extract text despite font variations
        assertThat(result).isNotNull();
        assertThat(result.extractedText()).isNotNull();
    }

    @Test
    void shouldReturnEmptyResultForImageWithoutText() {
        // Given: An image with no text (photograph, drawing)
        ImageData noTextImage = createImageWithoutText();

        // When: OCR is performed
        VisionResult result = visionTemplate.extractText(noTextImage);

        // Then: Should return empty or minimal text
        assertThat(result).isNotNull();
        // May return empty string or whitespace, but should not crash
    }

    @Test
    void shouldExtractTextWithConfidenceScores() {
        // Given: An image with clear text
        ImageData textImage = createDocumentImageWithText("Sample Text");

        // When: OCR is performed
        VisionResult result = visionTemplate.extractText(textImage);

        // Then: Should provide confidence scores for text regions
        assertThat(result).isNotNull();
        assertThat(result.detections()).isNotEmpty();

        result.detections().forEach(detection -> {
            assertThat(detection.confidence()).isBetween(0.0, 1.0);
        });
    }

    @Test
    void shouldHandleRotatedText() {
        // Given: An image with slightly rotated text
        ImageData rotatedTextImage = createRotatedTextImage();

        // When: OCR is performed
        VisionResult result = visionTemplate.extractText(rotatedTextImage);

        // Then: Should still extract text despite rotation
        assertThat(result).isNotNull();
        // May not be perfect, but should extract some text
    }

    @Test
    void shouldPreserveTextLayout() {
        // Given: An image with structured text (paragraphs, columns)
        ImageData structuredTextImage = createStructuredTextImage();

        // When: OCR is performed
        VisionResult result = visionTemplate.extractText(structuredTextImage);

        // Then: Should preserve basic text structure
        assertThat(result).isNotNull();
        assertThat(result.extractedText()).isNotNull();
        assertThat(result.extractedText().length()).isGreaterThan(10);
    }

    // Helper methods to create test images
    private ImageData createDocumentImageWithText(String text) {
        try {
            BufferedImage image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, 400, 200);

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            FontMetrics fm = g2d.getFontMetrics();

            String[] lines = text.split("\n");
            int y = 30;
            for (String line : lines) {
                g2d.drawString(line, 20, y);
                y += fm.getHeight();
            }

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return ImageData.fromBytes(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create text image", e);
        }
    }

    private ImageData createImageWithVariedFonts() {
        try {
            BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, 400, 300);

            g2d.setColor(Color.BLACK);

            // Different font sizes
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("Small text", 20, 30);

            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Medium text", 20, 70);

            g2d.setFont(new Font("Arial", Font.PLAIN, 28));
            g2d.drawString("Large text", 20, 110);

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return ImageData.fromBytes(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create varied font image", e);
        }
    }

    private ImageData createImageWithoutText() {
        try {
            BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            // Create a gradient background without text
            GradientPaint gradient = new GradientPaint(0, 0, Color.BLUE, 200, 200, Color.GREEN);
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, 200, 200);

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return ImageData.fromBytes(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create no-text image", e);
        }
    }

    private ImageData createRotatedTextImage() {
        // TODO: Create image with rotated text
        return createDocumentImageWithText("Rotated Text");
    }

    private ImageData createStructuredTextImage() {
        // TODO: Create image with structured text layout
        return createDocumentImageWithText("This is a paragraph.\n\nThis is another paragraph.\nWith multiple lines.");
    }
}
