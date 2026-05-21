package io.github.codesapienbe.springvision.core.djl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 * Direct wrapper around Tess4J. Tess4J is a mandatory dependency of
 * {@code spring-vision-core}; OCR and identity-document recognition fail loudly
 * if the native Tesseract library or {@code tessdata} directory is missing.
 *
 * <p>For identity documents the language combo must include the document's
 * printed languages. For Belgium that is {@code fra+nld+deu+eng}; for the
 * Netherlands {@code nld+eng}. Default language ({@code eng}) is used when
 * callers don't override it.</p>
 *
 * <p>The runner resolves the Tesseract {@code datapath} in this order:
 * explicit constructor argument, {@code TESSDATA_PREFIX} env var, common
 * platform locations ({@code /opt/homebrew/share/tessdata},
 * {@code /usr/local/share/tessdata}, {@code /usr/share/tesseract-ocr/&#42;/tessdata},
 * {@code /usr/share/tessdata}).</p>
 */
public final class TesseractRunner {

    private static final Logger logger = LoggerFactory.getLogger(TesseractRunner.class);

    private static final List<String> CANDIDATE_DATAPATHS = List.of(
        "/opt/homebrew/share/tessdata",
        "/usr/local/share/tessdata",
        "/usr/share/tesseract-ocr/5/tessdata",
        "/usr/share/tesseract-ocr/4.00/tessdata",
        "/usr/share/tessdata"
    );

    private static final List<String> CANDIDATE_NATIVE_LIB_PATHS = List.of(
        "/opt/homebrew/lib",
        "/usr/local/lib",
        "/usr/lib/x86_64-linux-gnu",
        "/usr/lib"
    );

    private final String defaultLanguages;
    private final String datapath;

    public TesseractRunner(String defaultLanguages, String datapath) {
        this.defaultLanguages = blankToNull(defaultLanguages);
        this.datapath = resolveDatapath(blankToNull(datapath));
        ensureNativeLibraryPath();
    }

    /**
     * Builds a runner from environment variables.
     *
     * <ul>
     *   <li>{@code SPRING_VISION_OCR_TESSERACT_LANGUAGES} — language combo (e.g. {@code fra+nld+deu+eng})</li>
     *   <li>{@code TESSDATA_PREFIX} — directory containing trained data files (falls back to common platform locations)</li>
     * </ul>
     */
    public static TesseractRunner fromEnvironment() {
        String langs = System.getenv("SPRING_VISION_OCR_TESSERACT_LANGUAGES");
        String datapath = System.getenv("TESSDATA_PREFIX");
        return new TesseractRunner(langs, datapath);
    }

    /**
     * Always {@code true}: Tess4J is a mandatory compile-time dependency.
     * Retained for binary compatibility with earlier callers that probed for
     * the optional binding.
     */
    public boolean isAvailable() {
        return true;
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
        if (imageBytes == null || imageBytes.length == 0) {
            throw new VisionProcessingException(
                "Image bytes are required", "tesseract_invalid_input", "OCR", null);
        }
        Tesseract tess = new Tesseract();
        if (datapath != null) {
            tess.setDatapath(datapath);
        }
        String languages = languageOverride != null ? languageOverride : defaultLanguages;
        if (languages != null) {
            tess.setLanguage(languages.toLowerCase(Locale.ROOT));
        }
        BufferedImage img;
        try {
            img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException ex) {
            throw new VisionProcessingException(
                "OCR image decode failed: " + ex.getMessage(),
                "tesseract_decode_failed", "OCR", ex);
        }
        if (img == null) {
            throw new VisionProcessingException(
                "Could not decode image for OCR", "tesseract_decode_failed", "OCR", null);
        }
        try {
            String result = tess.doOCR(img);
            return result == null ? "" : result;
        } catch (TesseractException ex) {
            throw new VisionProcessingException(
                "Tesseract OCR failed: " + ex.getMessage(),
                "tesseract_failed", "OCR", ex);
        }
    }

    String defaultLanguages() {
        return defaultLanguages;
    }

    String datapath() {
        return datapath;
    }

    /**
     * Ensure {@code jna.library.path} contains the directory holding
     * {@code libtesseract.dylib} / {@code libtesseract.so}. Tess4J relies on
     * JNA to discover the native library, and JNA does not search Homebrew's
     * default install locations on macOS.
     */
    private static void ensureNativeLibraryPath() {
        String existing = System.getProperty("jna.library.path");
        for (String candidate : CANDIDATE_NATIVE_LIB_PATHS) {
            if (!Files.isDirectory(Path.of(candidate))) {
                continue;
            }
            if (existing == null || existing.isBlank()) {
                System.setProperty("jna.library.path", candidate);
                logger.debug("Set jna.library.path={}", candidate);
                return;
            }
            if (!existing.contains(candidate)) {
                String updated = existing + java.io.File.pathSeparator + candidate;
                System.setProperty("jna.library.path", updated);
                logger.debug("Extended jna.library.path with {}", candidate);
            }
            return;
        }
    }

    private static String resolveDatapath(String explicit) {
        if (explicit != null && Files.isDirectory(Path.of(explicit))) {
            return explicit;
        }
        for (String candidate : CANDIDATE_DATAPATHS) {
            if (Files.isDirectory(Path.of(candidate))) {
                logger.debug("Tesseract datapath resolved to {}", candidate);
                return candidate;
            }
        }
        logger.debug("No tessdata directory found; relying on Tess4J built-in default");
        return null;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
