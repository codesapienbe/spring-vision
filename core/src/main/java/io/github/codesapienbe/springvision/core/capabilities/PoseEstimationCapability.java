package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

import java.util.List;

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
