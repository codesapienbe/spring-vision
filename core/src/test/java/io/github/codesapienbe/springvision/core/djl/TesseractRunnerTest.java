package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;

/**
 * Tesseract through the bytedeco natives and the bundled tessdata; skipped when the build did not
 * fetch the language data (-Pdownload-models).
 */
@DisplayName("TesseractRunner")
class TesseractRunnerTest {

    private TesseractRunner runner;

    @BeforeEach
    void setUp() {
        runner = new TesseractRunner(null, null);
        assumeTrue(runner.isAvailable(), "bundled tessdata not present — build with -Pdownload-models");
    }

    private static byte[] render(String... lines) throws IOException {
        BufferedImage image = new BufferedImage(900, 80 + lines.length * 90, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 48));
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], 40, 90 + i * 90);
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    @DisplayName("recognizeLines returns one box per line, top to bottom, with a real confidence")
    void recognisesLinesWithBoxes() throws IOException {
        TesseractRunner.PageText page = runner.recognizeLines(render("HELLO WORLD", "SPRING VISION 2026"), null);
        List<TesseractRunner.TextLine> lines = page.lines();

        assertThat(page.width()).isEqualTo(900);

        assertThat(lines).extracting(TesseractRunner.TextLine::text)
            .containsExactly("HELLO WORLD", "SPRING VISION 2026");
        assertThat(lines.get(0).y()).isLessThan(lines.get(1).y());
        assertThat(lines).allSatisfy(line -> {
            assertThat(line.confidence()).isBetween(0.5, 1.0);
            assertThat(line.width()).isPositive();
            assertThat(line.height()).isPositive();
        });
    }

    @Test
    @DisplayName("extractText reads text with a language combo")
    void extractsTextWithLanguageCombo() throws IOException {
        assertThat(runner.extractText(render("BELGIQUE BELGIE"), "fra+nld+deu+eng")).contains("BELGIQUE");
    }

    @Test
    @DisplayName("fails loudly for a language without data")
    void failsForMissingLanguage() throws IOException {
        byte[] image = render("HELLO");
        assertThatThrownBy(() -> runner.extractText(image, "jpn"))
            .isInstanceOf(VisionProcessingException.class)
            .hasMessageContaining("jpn");
    }

    @Test
    @DisplayName("reads Dutch first by default, then English, French and German")
    void readsDutchFirstByDefault() {
        // Dutch documents are the main use; English alone misreads Dutch words (see the class comment)
        assertThat(new TesseractRunner(null, null).defaultLanguages()).isEqualTo("nld+eng+fra+deu");
    }
}
