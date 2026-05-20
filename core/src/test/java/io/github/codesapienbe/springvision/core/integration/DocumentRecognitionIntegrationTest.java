package io.github.codesapienbe.springvision.core.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.capabilities.IdentityCardRecognitionCapability;
import io.github.codesapienbe.springvision.core.capabilities.OcrCapability;
import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;
import io.github.codesapienbe.springvision.core.djl.TesseractRunner;

/**
 * End-to-end test that proves the document recognition pipeline runs OCR
 * against the host Tesseract installation and parses a Belgian eID front into
 * structured fields. Requires the native Tesseract library and the
 * {@code fra+nld+deu+eng} language packs on the host.
 */
class DocumentRecognitionIntegrationTest {

    private static DjlVisionBackend backend;

    @BeforeAll
    static void initBackend() {
        backend = new DjlVisionBackend();
        backend.initialize();
        backend.setTesseractRunner(new TesseractRunner("fra+nld+deu+eng", null));
    }

    @Test
    void extractsTextFromRenderedImage() throws IOException {
        String content = "Hello Tesseract OCR 12345";
        byte[] png = renderText(new String[] {content}, 48);
        ImageData image = ImageData.fromBytes(png, "image/png");

        List<OcrCapability.TextDetection> detections = ((OcrCapability) backend).extractText(image);
        assertThat(detections).isNotEmpty();
        String combined = detections.stream()
            .map(OcrCapability.TextDetection::text)
            .reduce("", (a, b) -> a + "\n" + b);
        System.out.println("Extracted OCR text: " + combined);
        assertThat(combined.toLowerCase()).contains("hello").contains("tesseract");
    }

    @Test
    void recognizesBelgianEidFromRenderedFrontOfCard() throws IOException {
        String[] lines = {
            "BELGIQUE  BELGIE  BELGIEN",
            "CARTE D'IDENTITE / IDENTITEITSKAART",
            "NOM: MUSTERMANN",
            "PRENOMS: MAX",
            "NATIONALITE: BEL",
            "DATE DE NAISSANCE: 30.07.1985",
            "SEXE: M",
            "VALABLE JUSQU'AU: 15.03.2028",
            "591-1234567-89",
            "85.07.30-033.59"
        };
        byte[] png = renderText(lines, 36);
        ImageData image = ImageData.fromBytes(png, "image/png");

        List<Detection> detections = ((IdentityCardRecognitionCapability) backend)
            .recognizeIdentityCard(image, "BE");

        assertThat(detections).hasSize(1);
        Detection det = detections.get(0);
        assertThat(det.label()).isEqualTo("BELGIAN_EID");

        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) det.attributes().get("fields");
        System.out.println("Belgian eID fields: " + fields);
        System.out.println("Raw OCR:\n" + det.attributes().get("rawText"));

        // OCR is noisy; we only insist on the strongly-anchored fields.
        assertThat(fields).isNotNull();
        assertThat(fields.get("nationality")).isEqualTo("BEL");
        assertThat(fields.get("surname")).containsIgnoringCase("MUSTERMANN");
    }

    private static byte[] renderText(String[] lines, int fontSize) throws IOException {
        Font font = new Font(Font.MONOSPACED, Font.BOLD, fontSize);
        int padding = 40;
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D pg = probe.createGraphics();
        pg.setFont(font);
        var fm = pg.getFontMetrics();
        int lineHeight = fm.getHeight();
        int maxWidth = 0;
        for (String l : lines) {
            maxWidth = Math.max(maxWidth, fm.stringWidth(l));
        }
        pg.dispose();

        int width = padding * 2 + maxWidth;
        int height = padding * 2 + lineHeight * lines.length;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.BLACK);
            g.setFont(font);
            int y = padding + fm.getAscent();
            for (String l : lines) {
                g.drawString(l, padding, y);
                y += lineHeight;
            }
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
