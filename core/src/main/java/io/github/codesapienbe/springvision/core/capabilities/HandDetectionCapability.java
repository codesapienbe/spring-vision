package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for hand detection.
 *
 * <p>Backends implementing this interface explicitly advertise support for
 * hand detection independent of the broader {@code VisionBackend} SPI.</p>
 */
public interface HandDetectionCapability {

    /**
     * Detects hands in the provided image.
     */
    List<Detection> detectHands(ImageData imageData);
}
