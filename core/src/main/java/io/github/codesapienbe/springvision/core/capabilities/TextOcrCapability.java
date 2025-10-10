package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for text OCR.
 *
 * <p>Backends implementing this interface explicitly advertise support for
 * text OCR independent of the broader {@code VisionBackend} SPI.</p>
 */
public interface TextOcrCapability {

    /**
     * Detects text in the provided image.
     */
    List<Detection> detectText(ImageData imageData);
}
