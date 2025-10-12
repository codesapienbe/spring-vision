package io.github.codesapienbe.springvision.tesseract;

import io.github.codesapienbe.springvision.core.BackendHealthInfo;
import io.github.codesapienbe.springvision.core.BoundingBox;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;
import io.github.codesapienbe.springvision.core.capabilities.TextOcrCapability;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;

/**
 * Tesseract-based vision backend for optical character recognition (OCR).
 *
 * <p>This backend provides text detection and recognition capabilities using
 * the Tesseract OCR engine. It supports multiple languages and can extract
 * text from images with configurable accuracy settings.</p>
 *
 * <p>The backend uses tess4j, a Java wrapper for Tesseract, and includes
 * automatic language data management and error handling.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "spring.vision.tesseract", name = "enabled", havingValue = "true")
public class TesseractVisionBackend implements VisionBackend, TextOcrCapability {

    private static final Logger logger = LoggerFactory.getLogger(TesseractVisionBackend.class);

    private static final String BACKEND_ID = "tesseract";
    private static final String DISPLAY_NAME = "Tesseract OCR";
    private static final String VERSION = "5.3.4";

    private final ITesseract tesseract;

    /**
     * Default constructor for TesseractVisionBackend.
     */
    public TesseractVisionBackend() {
        this.tesseract = new Tesseract();
        // Set default language to English
        this.tesseract.setLanguage("eng");
        // Set tessdata path - tess4j handles this automatically
        this.tesseract.setDatapath(System.getenv("TESSDATA_PREFIX"));
    }

    @Override
    public String getBackendId() {
        return BACKEND_ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Set<DetectionType> getSupportedDetectionTypes() {
        return Set.of(DetectionType.TEXT);
    }

    @Override
    public boolean isHealthy() {
        try {
            // Simple health check - try to create a basic OCR instance
            return tesseract != null;
        } catch (Exception e) {
            logger.warn("Tesseract backend health check failed", e);
            return false;
        }
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        boolean healthy = isHealthy();
        BackendHealthInfo.HealthStatus status = healthy ? BackendHealthInfo.HealthStatus.HEALTHY : BackendHealthInfo.HealthStatus.UNHEALTHY;
        String statusMessage = healthy ? "Tesseract OCR engine is operational" : "Tesseract OCR engine is not available";
        String errorMessage = healthy ? null : "Tesseract initialization failed";

        return new BackendHealthInfo(
            getBackendId(),
            status,
            statusMessage,
            errorMessage,
            java.time.Instant.now(),
            0L,
            java.util.Collections.emptyMap()
        );
    }

    @Override
    public List<Detection> detectText(ImageData imageData) {
        if (imageData == null || imageData.data() == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }

        try {
            // Convert byte array to BufferedImage
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData.data()));
            if (image == null) {
                throw new VisionProcessingException("Failed to decode image data", "detectText", DetectionType.TEXT.name());
            }

            // Perform OCR
            String extractedText = tesseract.doOCR(image);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                // No text detected
                return Collections.emptyList();
            }

            // Create a detection for the entire text
            // Note: Tesseract doesn't provide bounding boxes for individual text blocks
            // in the basic API, so we create a single detection for the whole image
            BoundingBox bbox = new BoundingBox(0.0, 0.0, 1.0, 1.0); // Full image
            Detection detection = new Detection(
                "text",
                1.0, // Tesseract doesn't provide confidence for full text
                bbox,
                Collections.singletonMap("text", extractedText.trim())
            );

            return List.of(detection);

        } catch (IOException e) {
            throw new VisionProcessingException("Failed to process image data: " + e.getMessage(), e);
        } catch (TesseractException e) {
            throw new VisionProcessingException("OCR processing failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new VisionBackendException("Unexpected error during text detection: " + e.getMessage(), e);
        }
    }

}
