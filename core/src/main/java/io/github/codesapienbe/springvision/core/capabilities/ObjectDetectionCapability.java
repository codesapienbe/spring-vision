package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for object detection.
 *
 * <p>Backends implementing this interface explicitly advertise support for
 * object detection independent of the broader {@code VisionBackend} SPI.</p>
 */
public interface ObjectDetectionCapability {

    /**
     * Detects objects in the provided image.
     */
    List<Detection> detectObjects(ImageData imageData);
}
