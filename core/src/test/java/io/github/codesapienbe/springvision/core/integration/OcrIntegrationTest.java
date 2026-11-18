package io.github.codesapienbe.springvision.core.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;
import io.github.codesapienbe.springvision.core.djl.TesseractRunner;

/**
 * Integration test for OCR functionality. OCR runs on the bundled Tesseract natives and language
 * data, so it works offline; it is skipped when the build did not fetch the language data.
 */
public class OcrIntegrationTest {

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
    void readsTextOfflineWithNormalisedLineBoxes() {
        assumeTrue(new TesseractRunner(null, null).isAvailable(), "bundled tessdata not present");
        ImageData documentImage = createDocumentImageWithText("Hello World");

        VisionResult result = visionTemplate.extractText(documentImage);

        assertThat(result.detections()).isNotEmpty();
        Detection line = result.detections().get(0);
        assertThat(line.label()).containsIgnoringCase("hello");
        assertThat(line.confidence()).isBetween(0.0, 1.0);
        assertThat(line.boundingBox().width()).isPositive().isLessThanOrEqualTo(1.0);
    }

    @Test
    void returnsNoTextForAnImageWithoutText() {
        assumeTrue(new TesseractRunner(null, null).isAvailable(), "bundled tessdata not present");
        ImageData blank = createDocumentImageWithText("");

        assertThat(visionTemplate.extractText(blank).detections()).isEmpty();
    }

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
}
