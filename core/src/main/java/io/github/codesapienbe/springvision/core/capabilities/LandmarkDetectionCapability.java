package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Capability interface for landmark detection.
 *
 * <p>Backends implementing this interface explicitly advertise support for
 * landmark detection independent of the broader {@code VisionBackend} SPI.</p>
 */
public interface LandmarkDetectionCapability {

    /**
     * Detects landmarks in the provided image.
     */
    List<Detection> detectLandmarks(ImageData imageData);
}
