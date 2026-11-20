package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.capabilities.OcrCapability;
import io.github.codesapienbe.springvision.core.document.DocumentContent;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;

/** Documents with a text layer: no OCR involved, so no Tesseract language data is needed. */
class DocumentReaderTest {

    private final DocumentReader reader = new DocumentReader(new TesseractRunner(null, null));

    static byte[] textPdf(List<List<String>> pages) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (List<String> lines : pages) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(font, 12);
                    content.newLineAtOffset(72, 720);
                    for (String line : lines) {
                        content.showText(line);
                        content.newLineAtOffset(0, -18);
                    }
                    content.endText();
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("a digital PDF is read from its own text layer, page by page, in reading order")
    void textLayer() throws IOException {
        byte[] pdf = textPdf(List.of(
            List.of("CERTIFICATE OF CONFORMITY", "0.1 Make: Example Motors"),
            List.of("0.2.1 Commercial name: Example One", "Vehicle identification number: WEX1234567890ABCD")));

        List<OcrCapability.TextDetection> lines = reader.read(pdf);

        assertThat(lines).extracting(OcrCapability.TextDetection::text).containsExactly(
            "CERTIFICATE OF CONFORMITY", "0.1 Make: Example Motors",
            "0.2.1 Commercial name: Example One", "Vehicle identification number: WEX1234567890ABCD");
        assertThat(lines).extracting(line -> line.attributes().get("page")).containsExactly(1, 1, 2, 2);
        assertThat(lines).allSatisfy(line -> {
            assertThat(line.attributes()).containsEntry("source", "pdf-text");
            assertThat(line.confidence()).isEqualTo(1.0);
            assertThat(line.boundingBox()).isNull();
        });
    }

    @Test
    @DisplayName("a Word document is read through Tika")
    void wordDocument() throws IOException {
        byte[] docx = minimalDocx("Invoice 2026-118", "Total due: 42.00 EUR");

        assertThat(DocumentReader.isDocument(docx)).isTrue();
        List<OcrCapability.TextDetection> lines = reader.read(docx);

        assertThat(lines).extracting(OcrCapability.TextDetection::text)
            .containsExactly("Invoice 2026-118", "Total due: 42.00 EUR");
        assertThat(lines).allSatisfy(line -> assertThat(line.attributes()).containsEntry("source", "document-text"));
    }

    /**
     * The smallest valid .docx: a zip with the content types, the package relationship and the
     * document part. Built by hand because POI's writer needs the full OOXML schema jar, which Tika
     * (and so spring-vision) does not ship; Tika's own Word reader does not need it.
     */
    static byte[] minimalDocx(String... paragraphs) throws IOException {
        StringBuilder body = new StringBuilder();
        for (String paragraph : paragraphs) {
            body.append(paragraph(paragraph));
        }
        return docx(body.toString());
    }

    private static String paragraph(String text) {
        return "<w:p><w:r><w:t>" + text + "</w:t></w:r></w:p>";
    }

    private static byte[] docx(String body) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            entry(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/word/document.xml\" ContentType=\"application/"
                + "vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
            entry(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/"
                + "relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>");
            entry(zip, "word/document.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:body>" + body + "</w:body></w:document>");
        }
        return out.toByteArray();
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    @Test
    @DisplayName("a password-protected PDF is refused with a clear message")
    void encrypted() throws IOException {
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            StandardProtectionPolicy policy = new StandardProtectionPolicy("owner", "secret", new AccessPermission());
            document.protect(policy);
            document.save(out);
            pdf = out.toByteArray();
        }

        assertThatThrownBy(() -> reader.read(pdf))
            .isInstanceOf(VisionProcessingException.class)
            .hasMessageContaining("password-protected");
    }

    @Test
    @DisplayName("a PDF over the page limit is refused instead of read in part")
    void tooLong() throws IOException {
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i <= DocumentReader.MAX_PAGES; i++) {
                document.addPage(new PDPage());
            }
            document.save(out);
            pdf = out.toByteArray();
        }

        assertThatThrownBy(() -> reader.read(pdf))
            .isInstanceOf(VisionProcessingException.class)
            .hasMessageContaining((DocumentReader.MAX_PAGES + 1) + " pages");
    }

    @Test
    @DisplayName("documents are told apart from images by their signature")
    void detection() throws IOException {
        assertThat(DocumentReader.isDocument(textPdf(List.of(List.of("x"))))).isTrue();
        assertThat(DocumentReader.isDocument(new byte[] {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, 0, 0, 0, 0, 0}))
            .isTrue();
        assertThat(DocumentReader.isDocument(new byte[] {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3})).isFalse();
        assertThat(DocumentReader.isDocument(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 1})).isFalse();
    }

    @Test
    @DisplayName("a Word table comes back as rows of cells, next to the text")
    void wordTable() throws IOException {
        String table = "<w:tbl>"
            + "<w:tr><w:tc>" + paragraph("Item") + "</w:tc><w:tc>" + paragraph("Price") + "</w:tc></w:tr>"
            + "<w:tr><w:tc>" + paragraph("Front tyre") + paragraph("225/55R18") + "</w:tc><w:tc>" + paragraph("120.00")
            + "</w:tc></w:tr>"
            + "<w:tr><w:tc>" + paragraph("") + "</w:tc><w:tc>" + paragraph("") + "</w:tc></w:tr>"
            + "</w:tbl>";

        DocumentContent content = reader.readContent(docx(paragraph("Quote: Q-17") + table));

        assertThat(content.tables()).hasSize(1);
        assertThat(content.tables().getFirst().page()).isNull();
        assertThat(content.tables().getFirst().rows()).containsExactly(
            List.of("Item", "Price"), List.of("Front tyre 225/55R18", "120.00"));
        assertThat(content.fields()).extracting(field -> field.label() + "=" + field.value()).contains("Quote=Q-17");
        assertThat(content.formFields()).isEmpty();
    }

    @Test
    @DisplayName("a PDF form's filled-in fields and the PDF's properties are returned; blank fields are left out")
    void pdfFormAndMetadata() throws IOException {
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 720);
                content.showText("Rental agreement for an example vehicle");
                content.endText();
            }
            PDAcroForm form = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(form);
            PDResources resources = new PDResources();
            resources.put(COSName.getPDFName("Helv"), new PDType1Font(Standard14Fonts.FontName.HELVETICA));
            form.setDefaultResources(resources);
            form.setDefaultAppearance("/Helv 0 Tf 0 g");
            for (PDTextField field : List.of(textField(form, page, "plate", "1-ABC-123", 650),
                textField(form, page, "notes", null, 600))) {
                form.getFields().add(field);
                page.getAnnotations().add(field.getWidgets().getFirst());
            }

            Calendar created = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            created.clear();
            created.set(2026, Calendar.MARCH, 4, 10, 0, 0);
            document.getDocumentInformation().setTitle("Example agreement");
            document.getDocumentInformation().setAuthor("Example Rentals");
            document.getDocumentInformation().setCreationDate(created);
            document.save(out);
            pdf = out.toByteArray();
        }

        DocumentContent content = reader.readContent(pdf);

        assertThat(content.formFields()).containsExactly(Map.entry("plate", "1-ABC-123"));
        assertThat(content.metadata()).containsExactly(
            Map.entry("title", "Example agreement"), Map.entry("author", "Example Rentals"),
            Map.entry("created", "2026-03-04T10:00:00Z"), Map.entry("pages", "1"));
        assertThat(content.tables()).isEmpty();
    }

    private static PDTextField textField(PDAcroForm form, PDPage page, String name, String value, float y)
        throws IOException {
        PDTextField field = new PDTextField(form);
        field.setPartialName(name);
        PDAnnotationWidget widget = field.getWidgets().getFirst();
        widget.setRectangle(new PDRectangle(72, y, 200, 20));
        widget.setPage(page);
        if (value != null) {
            field.setValue(value);
        }
        return field;
    }
}
