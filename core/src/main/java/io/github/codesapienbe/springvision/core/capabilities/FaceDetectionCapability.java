package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Capability interface for face detection.
 *
 * <p>Backends implementing this interface explicitly advertise support for
 * face detection independent of the broader {@code VisionBackend} SPI.</p>
 */
public interface FaceDetectionCapability {

    /**
     * Detects faces in the provided image.
     *
     * @param imageData The image to process.
     * @return A list of detected faces.
     */
    List<Detection> detectFaces(ImageData imageData);
}
