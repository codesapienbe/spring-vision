package io.github.codesapienbe.springvision.core.djl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;

/**
 * Reflective wrapper around the optional Tess4J library.
 *
 * <p>Tess4J is not a compile-time dependency of {@code spring-vision-core}: the
 * project uses DJL's {@code WORD_RECOGNITION} for general OCR and only falls
 * back to Tess4J when DJL fails (or when callers explicitly want multi-language
 * document OCR). All calls are made via reflection so the core module compiles
 * and tests even when Tess4J is absent from the classpath.</p>
 *
 * <p>For identity documents the language combo must include the document's
 * printed languages. For Belgium that is {@code fra+nld+deu+eng}; for the
 * Netherlands {@code nld+eng}. Default language ({@code eng}) is used when
 * callers don't override it.</p>
 *
 * <p>The runner also accepts a Tesseract {@code datapath} (directory containing
 * {@code .traineddata} files). When {@code null}, Tess4J falls back to the
 * {@code TESSDATA_PREFIX} environment variable or its compiled-in default.</p>
 */
public final class TesseractRunner {

    private static final Logger logger = LoggerFactory.getLogger(TesseractRunner.class);

    private static final String TESSERACT_CLASS = "net.sourceforge.tess4j.Tesseract";

    private final String defaultLanguages;
    private final String datapath;
    private final boolean available;

    public TesseractRunner(String defaultLanguages, String datapath) {
        this.defaultLanguages = blankToNull(defaultLanguages);
        this.datapath = blankToNull(datapath);
        this.available = isTesseractOnClasspath();
    }

    /**
     * Builds a runner from environment variables.
     *
     * <ul>
     *   <li>{@code SPRING_VISION_OCR_TESSERACT_LANGUAGES} — language combo (e.g. {@code fra+nld+deu+eng})</li>
     *   <li>{@code TESSDATA_PREFIX} — directory containing trained data files</li>
     * </ul>
     */
    public static TesseractRunner fromEnvironment() {
        String langs = System.getenv("SPRING_VISION_OCR_TESSERACT_LANGUAGES");
        String datapath = System.getenv("TESSDATA_PREFIX");
        return new TesseractRunner(langs, datapath);
    }

    /** @return true if Tess4J is on the classpath. */
    public boolean isAvailable() {
        return available;
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
        if (!available) {
            throw new VisionProcessingException(
                "Tess4J is not on the classpath; identity-document OCR requires it",
                "tesseract_unavailable", "OCR", null);
        }
        if (imageBytes == null || imageBytes.length == 0) {
            throw new VisionProcessingException(
                "Image bytes are required", "tesseract_invalid_input", "OCR", null);
        }
        try {
            Class<?> tesseractClass = Class.forName(TESSERACT_CLASS);
            Object tess = tesseractClass.getConstructor().newInstance();

            if (datapath != null) {
                Method setDatapath = tesseractClass.getMethod("setDatapath", String.class);
                setDatapath.invoke(tess, datapath);
            }
            String languages = languageOverride != null ? languageOverride : defaultLanguages;
            if (languages != null) {
                Method setLanguage = tesseractClass.getMethod("setLanguage", String.class);
                setLanguage.invoke(tess, languages.toLowerCase(Locale.ROOT));
            }

            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) {
                throw new VisionProcessingException(
                    "Could not decode image for OCR", "tesseract_decode_failed", "OCR", null);
            }
            Method doOcr = tesseractClass.getMethod("doOCR", BufferedImage.class);
            Object result = doOcr.invoke(tess, img);
            return result == null ? "" : result.toString();
        } catch (ClassNotFoundException ex) {
            throw new VisionProcessingException(
                "Tess4J class not found at runtime: " + ex.getMessage(),
                "tesseract_unavailable", "OCR", ex);
        } catch (IOException ex) {
            throw new VisionProcessingException(
                "OCR image decode failed: " + ex.getMessage(),
                "tesseract_decode_failed", "OCR", ex);
        } catch (ReflectiveOperationException ex) {
            // Unwrap InvocationTargetException so the user sees the real Tess4J error.
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new VisionProcessingException(
                "Tesseract OCR failed: " + cause.getMessage(),
                "tesseract_failed", "OCR", cause);
        }
    }

    String defaultLanguages() {
        return defaultLanguages;
    }

    String datapath() {
        return datapath;
    }

    private static boolean isTesseractOnClasspath() {
        try {
            Class.forName(TESSERACT_CLASS);
            return true;
        } catch (ClassNotFoundException ex) {
            logger.debug("Tess4J not on classpath: {}", ex.getMessage());
            return false;
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
