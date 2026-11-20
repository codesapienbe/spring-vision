package io.github.codesapienbe.springvision.core.djl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDPushButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTerminalField;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.PagedText;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.TeeContentHandler;
import org.xml.sax.SAXException;

import io.github.codesapienbe.springvision.core.capabilities.OcrCapability;
import io.github.codesapienbe.springvision.core.document.DocumentContent;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;

/**
 * Reads the text of PDF and Microsoft Office documents.
 *
 * <p>A PDF page's own text layer is used when it has one: it is the exact text, and reading it is
 * far faster than OCR. A page without text (a scan) is rendered at {@value #OCR_DPI} DPI and read
 * by Tesseract; scanned pages are read in parallel. Word, Excel and PowerPoint documents go through
 * Apache Tika. Every line says which page it is on and where it came from ({@code pdf-text},
 * {@code ocr} or {@code document-text}).</p>
 *
 * <p>{@link #readContent(byte[])} also returns what the file itself declares: the tables of an
 * Office document, the filled-in fields of a PDF form, and the document's properties.</p>
 */
final class DocumentReader {

    static final int MAX_PAGES = 50;
    static final float OCR_DPI = 300f;
    /** Fewer letters and digits than this on a page means it is a scan, not a text page. */
    private static final int MIN_TEXT_CHARACTERS = 10;
    private static final int OCR_THREADS = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));

    private final TesseractRunner ocr;

    DocumentReader(TesseractRunner ocr) {
        this.ocr = ocr;
    }

    /** Whether the bytes are a PDF or a Microsoft Office document rather than an image. */
    static boolean isDocument(byte[] data) {
        return isPdf(data) || isZip(data) || isOle2(data);
    }

    static boolean isPdf(byte[] data) {
        return data.length > 4 && data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F';
    }

    /** Word, Excel and PowerPoint since 2007 (docx, xlsx, pptx) are zip files. */
    private static boolean isZip(byte[] data) {
        return data.length > 4 && data[0] == 'P' && data[1] == 'K' && data[2] == 3 && data[3] == 4;
    }

    /** The older Office formats (doc, xls, ppt) are OLE2 compound files. */
    private static boolean isOle2(byte[] data) {
        return data.length > 8 && (data[0] & 0xFF) == 0xD0 && (data[1] & 0xFF) == 0xCF
            && (data[2] & 0xFF) == 0x11 && (data[3] & 0xFF) == 0xE0;
    }

    /**
     * The document's text, one detection per line, in page order.
     *
     * @throws VisionProcessingException when the document cannot be read, is password-protected, or
     *     has more than {@value #MAX_PAGES} pages
     */
    List<OcrCapability.TextDetection> read(byte[] data) {
        return readContent(data).lines();
    }

    /**
     * The document's text lines plus its tables, form fields and properties.
     *
     * @throws VisionProcessingException when the document cannot be read, is password-protected, or
     *     has more than {@value #MAX_PAGES} pages
     */
    DocumentContent readContent(byte[] data) {
        try {
            return isPdf(data) ? readPdf(data) : readOffice(data);
        } catch (InvalidPasswordException e) {
            throw new VisionProcessingException("The PDF is password-protected", "document_encrypted", "OCR", e);
        } catch (IOException | TikaException | SAXException e) {
            throw new VisionProcessingException("Could not read the document: " + e.getMessage(),
                "document_unreadable", "OCR", e);
        }
    }

    private DocumentContent readPdf(byte[] data) throws IOException {
        try (PDDocument document = Loader.loadPDF(data)) {
            int pages = document.getNumberOfPages();
            if (pages > MAX_PAGES) {
                throw new VisionProcessingException("The PDF has " + pages + " pages; at most " + MAX_PAGES
                    + " are read. Split it and send the pages you need.", "document_too_long", "OCR", null);
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            List<OcrCapability.TextDetection> lines = new ArrayList<>();
            List<Integer> scanned = new ArrayList<>();
            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                if (countLettersAndDigits(text) >= MIN_TEXT_CHARACTERS) {
                    lines.addAll(textLines(text, page, "pdf-text"));
                } else {
                    scanned.add(page);
                }
            }
            lines.addAll(readScannedPages(document, scanned));
            lines.sort(Comparator.comparingInt(line -> (Integer) line.attributes().get("page")));
            return new DocumentContent(lines, List.of(), formFields(document), pdfMetadata(document));
        }
    }

    /** The filled-in fields of the PDF's form, by their full name; buttons, signatures and blanks left out. */
    private static Map<String, String> formFields(PDDocument document) {
        PDAcroForm form = document.getDocumentCatalog().getAcroForm();
        if (form == null) {
            return Map.of();
        }
        return StreamSupport.stream(form.getFieldTree().spliterator(), false)
            .filter(field -> field instanceof PDTerminalField
                && !(field instanceof PDPushButton) && !(field instanceof PDSignatureField))
            .filter(field -> field.getValueAsString() != null && !field.getValueAsString().isBlank()
                && !"Off".equals(field.getValueAsString()))
            .collect(Collectors.toMap(field -> field.getFullyQualifiedName(), field -> field.getValueAsString().strip(),
                (first, second) -> first, LinkedHashMap::new));
    }

    private static Map<String, String> pdfMetadata(PDDocument document) {
        PDDocumentInformation info = document.getDocumentInformation();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("title", info.getTitle());
        values.put("author", info.getAuthor());
        values.put("subject", info.getSubject());
        values.put("created", iso(info.getCreationDate()));
        values.put("modified", iso(info.getModificationDate()));
        values.put("pages", String.valueOf(document.getNumberOfPages()));
        return present(values);
    }

    private static String iso(Calendar calendar) {
        return calendar == null ? null : calendar.toInstant().toString();
    }

    /** Renders the scanned pages one by one (PDFBox renders a document on one thread) and OCRs them in parallel. */
    private List<OcrCapability.TextDetection> readScannedPages(PDDocument document, List<Integer> pages) throws IOException {
        if (pages.isEmpty()) {
            return List.of();
        }
        PDFRenderer renderer = new PDFRenderer(document);
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(OCR_THREADS, pages.size()),
            Thread.ofPlatform().daemon().name("spring-vision-pdf-ocr-", 0).factory());
        try {
            List<Future<List<OcrCapability.TextDetection>>> results = new ArrayList<>();
            for (int page : pages) {
                byte[] png = renderPage(renderer, page);
                results.add(pool.submit(() -> ocrLines(png, page)));
            }
            List<OcrCapability.TextDetection> lines = new ArrayList<>();
            for (Future<List<OcrCapability.TextDetection>> result : results) {
                lines.addAll(result.get());
            }
            return lines;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VisionProcessingException("Interrupted while reading the PDF", "interrupted", "OCR", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new VisionProcessingException("OCR of a PDF page failed: " + e.getCause().getMessage(),
                "ocr_failed", "OCR", e.getCause());
        } finally {
            pool.shutdownNow();
        }
    }

    private static byte[] renderPage(PDFRenderer renderer, int page) throws IOException {
        BufferedImage image = renderer.renderImageWithDPI(page - 1, OCR_DPI, ImageType.GRAY);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        return png.toByteArray();
    }

    private List<OcrCapability.TextDetection> ocrLines(byte[] png, int page) {
        TesseractRunner.PageText text = ocr.recognizeLines(png, null);
        List<OcrCapability.TextDetection> lines = new ArrayList<>(text.lines().size());
        for (TesseractRunner.TextLine line : text.lines()) {
            Map<String, Object> box = new HashMap<>();
            box.put("x", (double) line.x() / text.width());
            box.put("y", (double) line.y() / text.height());
            box.put("width", (double) line.width() / text.width());
            box.put("height", (double) line.height() / text.height());
            lines.add(new OcrCapability.TextDetection(line.text(), line.confidence(), box, attributes(page, "ocr")));
        }
        return lines;
    }

    private static DocumentContent readOffice(byte[] data) throws IOException, TikaException, SAXException {
        BodyContentHandler text = new BodyContentHandler(-1);
        TableCollector tables = new TableCollector();
        Metadata metadata = new Metadata();
        try (TikaInputStream stream = TikaInputStream.get(data)) {
            new AutoDetectParser().parse(stream, new TeeContentHandler(text, tables), metadata, new ParseContext());
        }
        return new DocumentContent(textLines(text.toString(), null, "document-text"), tables.tables(), Map.of(),
            officeMetadata(metadata));
    }

    private static Map<String, String> officeMetadata(Metadata metadata) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("title", metadata.get(TikaCoreProperties.TITLE));
        values.put("author", metadata.get(TikaCoreProperties.CREATOR));
        values.put("subject", metadata.get(TikaCoreProperties.SUBJECT));
        values.put("created", metadata.get(TikaCoreProperties.CREATED));
        values.put("modified", metadata.get(TikaCoreProperties.MODIFIED));
        values.put("pages", metadata.get(PagedText.N_PAGES));
        return present(values);
    }

    /** The entries that have a value, stripped, in the same order. */
    private static Map<String, String> present(Map<String, String> values) {
        return values.entrySet().stream()
            .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().strip(),
                (first, second) -> first, LinkedHashMap::new));
    }

    /**
     * One detection per non-blank line of text taken from the document itself. It is the document's
     * own text, hence confidence 1.0; it has no position, hence no bounding box.
     */
    private static List<OcrCapability.TextDetection> textLines(String text, Integer page, String source) {
        return text.lines()
            .map(String::strip)
            .filter(line -> !line.isEmpty())
            .map(line -> new OcrCapability.TextDetection(line, 1.0, null, attributes(page, source)))
            .toList();
    }

    private static Map<String, Object> attributes(Integer page, String source) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("source", source);
        attributes.put("level", "line");
        if (page != null) {
            attributes.put("page", page);
        }
        return attributes;
    }

    private static int countLettersAndDigits(String text) {
        return (int) text.chars().filter(Character::isLetterOrDigit).count();
    }
}
