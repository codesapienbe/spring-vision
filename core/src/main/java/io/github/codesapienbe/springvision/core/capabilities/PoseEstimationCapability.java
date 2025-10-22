package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Capability interface for pose estimation.
 *
 * <p>Backends implementing this interface explicitly advertise support for
 * pose estimation independent of the broader {@code VisionBackend} SPI.</p>
 */
public interface PoseEstimationCapability {

    /**
     * Detects poses in the provided image.
     */
    List<Detection> detectPoses(ImageData imageData);
}
