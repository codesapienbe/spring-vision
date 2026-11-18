package io.github.codesapienbe.springvision.core.djl;

import static org.bytedeco.leptonica.global.leptonica.pixDestroy;
import static org.bytedeco.leptonica.global.leptonica.pixGetHeight;
import static org.bytedeco.leptonica.global.leptonica.pixGetWidth;
import static org.bytedeco.leptonica.global.leptonica.pixReadMem;
import static org.bytedeco.tesseract.global.tesseract.PSM_SINGLE_LINE;
import static org.bytedeco.tesseract.global.tesseract.RIL_SYMBOL;
import static org.bytedeco.tesseract.global.tesseract.RIL_TEXTLINE;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.ResultIterator;
import org.bytedeco.tesseract.TessBaseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;

/**
 * Tesseract 5 OCR through the bytedeco JavaCPP preset. The natives ship with the jar, and the
 * language data ({@code eng}, {@code nld}, {@code fra}, {@code deu} from tessdata_fast) is
 * bundled under {@code /models/tessdata} by the {@code download-models} profile, so no host
 * install is needed.
 *
 * <p>For identity documents the language combo must include the document's printed languages.
 * For Belgium that is {@code fra+nld+deu+eng}; for the Netherlands {@code nld+eng}. The default
 * language ({@code eng}) is used when callers don't override it.</p>
 *
 * <p>The tessdata directory is resolved in this order: explicit constructor argument (for
 * example {@code TESSDATA_PREFIX} via {@link #fromEnvironment()}), then the bundled data.</p>
 */
public final class TesseractRunner {

    private static final Logger logger = LoggerFactory.getLogger(TesseractRunner.class);

    private static final String DEFAULT_LANGUAGES = "eng";
    private static final List<String> BUNDLED_LANGUAGES = List.of("eng", "nld", "fra", "deu");

    /**
     * One recognised text line.
     *
     * @param text       the line's text, trimmed
     * @param confidence Tesseract's own confidence for the line, scaled from 0-100 to 0-1
     * @param x          left edge in pixels
     * @param y          top edge in pixels
     * @param width      width in pixels
     * @param height     height in pixels
     */
    public record TextLine(String text, double confidence, int x, int y, int width, int height) {
    }

    /**
     * The lines recognised on one image, with the image's size so callers can normalise boxes.
     *
     * @param width  image width in pixels
     * @param height image height in pixels
     * @param lines  recognised lines, top to bottom
     */
    public record PageText(int width, int height, List<TextLine> lines) {

        public PageText {
            lines = List.copyOf(lines);
        }
    }

    private final String defaultLanguages;
    private final String datapath;

    public TesseractRunner(String defaultLanguages, String datapath) {
        String languages = blankToNull(defaultLanguages);
        this.defaultLanguages = languages == null ? DEFAULT_LANGUAGES : languages;
        this.datapath = resolveDatapath(blankToNull(datapath));
    }

    /**
     * Builds a runner from environment variables.
     *
     * <ul>
     *   <li>{@code SPRING_VISION_OCR_TESSERACT_LANGUAGES} — language combo (e.g. {@code fra+nld+deu+eng})</li>
     *   <li>{@code TESSDATA_PREFIX} — directory with {@code .traineddata} files, overriding the bundled data</li>
     * </ul>
     */
    public static TesseractRunner fromEnvironment() {
        String langs = System.getenv("SPRING_VISION_OCR_TESSERACT_LANGUAGES");
        String datapath = System.getenv("TESSDATA_PREFIX");
        return new TesseractRunner(langs, datapath);
    }

    /** Whether language data for the default languages is available. */
    public boolean isAvailable() {
        return missingLanguages(defaultLanguages).isEmpty();
    }

    /** Runs OCR using the default language combo. */
    public String extractText(byte[] imageBytes) {
        return extractText(imageBytes, defaultLanguages);
    }

    /**
     * Runs OCR with an explicit language override.
     *
     * @param imageBytes the encoded image
     * @param languageOverride Tesseract language combo (e.g. {@code fra+nld+deu+eng}); pass {@code null}
     *                         to use the default
     */
    public String extractText(byte[] imageBytes, String languageOverride) {
        return withTesseract(imageBytes, languageOverride, (api, pix) -> {
            BytePointer text = api.GetUTF8Text();
            if (text == null) {
                return "";
            }
            try {
                return text.getString(StandardCharsets.UTF_8);
            } finally {
                text.deallocate();
            }
        });
    }

    /**
     * Recognises text line by line, each with its box and Tesseract's confidence. Empty lines are
     * left out.
     */
    public PageText recognizeLines(byte[] imageBytes, String languageOverride) {
        return withTesseract(imageBytes, languageOverride, (api, pix) -> {
            if (api.Recognize(null) != 0) {
                throw new VisionProcessingException("Tesseract recognition failed", "tesseract_failed", "OCR", null);
            }
            int width = pixGetWidth(pix);
            int height = pixGetHeight(pix);
            ResultIterator lines = api.GetIterator();
            if (lines == null) {
                return new PageText(width, height, List.of());
            }
            List<TextLine> result = new ArrayList<>();
            try (IntPointer left = new IntPointer(1);
                IntPointer top = new IntPointer(1);
                IntPointer right = new IntPointer(1);
                IntPointer bottom = new IntPointer(1)) {
                do {
                    BytePointer raw = lines.GetUTF8Text(RIL_TEXTLINE);
                    if (raw == null) {
                        continue;
                    }
                    String text = raw.getString(StandardCharsets.UTF_8).strip();
                    raw.deallocate();
                    if (text.isEmpty() || !lines.BoundingBox(RIL_TEXTLINE, left, top, right, bottom)) {
                        continue;
                    }
                    double confidence = Math.max(0.0, Math.min(1.0, lines.Confidence(RIL_TEXTLINE) / 100.0));
                    result.add(new TextLine(text, confidence,
                        left.get(), top.get(), right.get() - left.get(), bottom.get() - top.get()));
                } while (lines.Next(RIL_TEXTLINE));
            }
            return new PageText(width, height, result);
        });
    }

    /**
     * Reads {@code imageBytes} as a single line of text restricted to {@code allowedCharacters}
     * (for example a licence-plate crop). The returned box covers the whole image, and the
     * confidence is that of the weakest recognised character.
     */
    public TextLine readLine(byte[] imageBytes, String languageOverride, String allowedCharacters) {
        return withTesseract(imageBytes, languageOverride, (api, pix) -> {
            api.SetPageSegMode(PSM_SINGLE_LINE);
            api.SetVariable("tessedit_char_whitelist", allowedCharacters);
            if (api.Recognize(null) != 0) {
                throw new VisionProcessingException("Tesseract recognition failed", "tesseract_failed", "OCR", null);
            }
            BytePointer raw = api.GetUTF8Text();
            String text = "";
            if (raw != null) {
                text = raw.getString(StandardCharsets.UTF_8).strip();
                raw.deallocate();
            }
            // The weakest character's confidence: one wrong character makes a whole plate wrong, and
            // Tesseract's word/line confidence can drop to 0 on a blurred crop it read correctly.
            double weakest = 1.0;
            boolean anySymbol = false;
            ResultIterator symbols = api.GetIterator();
            if (symbols != null) {
                do {
                    BytePointer symbol = symbols.GetUTF8Text(RIL_SYMBOL);
                    if (symbol == null) {
                        continue;
                    }
                    boolean blank = symbol.getString(StandardCharsets.UTF_8).isBlank();
                    symbol.deallocate();
                    if (!blank) {
                        weakest = Math.min(weakest, symbols.Confidence(RIL_SYMBOL) / 100.0);
                        anySymbol = true;
                    }
                } while (symbols.Next(RIL_SYMBOL));
            }
            double confidence = anySymbol ? Math.max(0.0, weakest) : 0.0;
            return new TextLine(text, confidence, 0, 0, pixGetWidth(pix), pixGetHeight(pix));
        });
    }

    private <T> T withTesseract(byte[] imageBytes, String languageOverride, BiFunction<TessBaseAPI, PIX, T> work) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new VisionProcessingException(
                "Image bytes are required", "tesseract_invalid_input", "OCR", null);
        }
        String languages = (languageOverride != null ? languageOverride : defaultLanguages).toLowerCase(Locale.ROOT);
        List<String> missing = missingLanguages(languages);
        if (!missing.isEmpty()) {
            throw new VisionProcessingException(
                "No Tesseract language data for " + missing + " (looked in " + datapath
                    + "); build with -Pdownload-models or set TESSDATA_PREFIX",
                "tesseract_language_missing", "OCR", null);
        }
        // TessBaseAPI is not thread-safe, so each call gets its own instance
        try (TessBaseAPI api = new TessBaseAPI(); BytePointer data = new BytePointer(imageBytes)) {
            if (api.Init(datapath, languages) != 0) {
                throw new VisionProcessingException(
                    "Tesseract could not initialise languages " + languages, "tesseract_init_failed", "OCR", null);
            }
            PIX pix = pixReadMem(data, imageBytes.length);
            if (pix == null) {
                api.End();
                throw new VisionProcessingException(
                    "Could not decode image for OCR", "tesseract_decode_failed", "OCR", null);
            }
            try {
                api.SetImage(pix);
                return work.apply(api, pix);
            } finally {
                pixDestroy(pix);
                api.End();
            }
        }
    }

    private List<String> missingLanguages(String languages) {
        if (datapath == null) {
            return Arrays.asList(languages.split("\\+"));
        }
        return Arrays.stream(languages.split("\\+"))
            .filter(lang -> !Files.isRegularFile(Path.of(datapath, lang + ".traineddata")))
            .toList();
    }

    String defaultLanguages() {
        return defaultLanguages;
    }

    String datapath() {
        return datapath;
    }

    private static String resolveDatapath(String explicit) {
        if (explicit != null && Files.isDirectory(Path.of(explicit))) {
            return explicit;
        }
        String bundled = null;
        for (String lang : BUNDLED_LANGUAGES) {
            String url = ClasspathModels.getModelUrl("tessdata/" + lang + ".traineddata");
            if (url != null) {
                bundled = Paths.get(java.net.URI.create(url)).getParent().toString();
            }
        }
        if (bundled == null) {
            logger.warn("No bundled Tesseract language data found; build with -Pdownload-models or set TESSDATA_PREFIX");
        }
        return bundled;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
