package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.github.codesapienbe.springvision.core.capabilities.OcrCapability;

/**
 * A scanned PDF (a page that is only an image) and a PDF mixing a text page with a scan, read with
 * the real Tesseract. Runs only when the language data is bundled ({@code -Pdownload-models}).
 */
@EnabledIf("tessdataAvailable")
class DocumentReaderOcrIntegrationTest {

    private final DocumentReader reader = new DocumentReader(new TesseractRunner(null, null));

    static boolean tessdataAvailable() {
        return ClasspathModels.isModelAvailable(ModelCatalog.TESSDATA_ENG);
    }

    /** A page that holds nothing but a picture of the given lines, like a scan. */
    static byte[] scannedPdf(List<String> lines) throws IOException {
        BufferedImage scan = new BufferedImage(1700, 2200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scan.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, scan.getWidth(), scan.getHeight());
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("SansSerif", Font.PLAIN, 44));
        int y = 200;
        for (String line : lines) {
            g.drawString(line, 150, y);
            y += 90;
        }
        g.dispose();
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDImageXObject image = LosslessFactory.createFromImage(document, scan);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(image, 0, 0, PDRectangle.LETTER.getWidth(), PDRectangle.LETTER.getHeight());
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("a scanned page is rendered and read with OCR, with page number and box")
    void scannedPage() throws IOException {
        byte[] pdf = scannedPdf(List.of("CERTIFICATE OF CONFORMITY", "Vehicle category M1"));

        long start = System.nanoTime();
        List<OcrCapability.TextDetection> lines = reader.read(pdf);
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertThat(String.join("\n", lines.stream().map(OcrCapability.TextDetection::text).toList()))
            .contains("CERTIFICATE OF CONFORMITY").contains("Vehicle category M1");
        assertThat(lines).allSatisfy(line -> {
            assertThat(line.attributes()).containsEntry("source", "ocr").containsEntry("page", 1);
            assertThat(line.boundingBox()).isNotNull();
        });
        System.out.printf("scanned page read with OCR in %d ms%n", millis);
    }

    @Test
    @DisplayName("text pages and scanned pages in one PDF keep their page order")
    void mixed() throws IOException {
        byte[] text = DocumentReaderTest.textPdf(List.of(List.of("Page one has a text layer")));
        byte[] scan = scannedPdf(List.of("Page two is a scan"));
        byte[] pdf;
        try (PDDocument first = org.apache.pdfbox.Loader.loadPDF(text);
             PDDocument second = org.apache.pdfbox.Loader.loadPDF(scan);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            first.importPage(second.getPage(0));
            first.save(out);
            pdf = out.toByteArray();
        }

        List<OcrCapability.TextDetection> lines = reader.read(pdf);

        assertThat(lines.getFirst().text()).isEqualTo("Page one has a text layer");
        assertThat(lines.getFirst().attributes()).containsEntry("source", "pdf-text").containsEntry("page", 1);
        assertThat(lines.getLast().attributes()).containsEntry("source", "ocr").containsEntry("page", 2);
        assertThat(lines.getLast().text()).contains("Page two is a scan");
    }
}
