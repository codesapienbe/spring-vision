package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Capability interface for barcode detection.
 *
 * <p>Backends implementing this interface explicitly advertise support for
 * barcode detection independent of the broader {@code VisionBackend} SPI.</p>
 */
public interface BarcodeCapability {

    /**
     * Detects barcodes in the provided image.
     * @param imageData The image data to process.
     * @return A list of detections, each representing a found barcode.
     */
    List<Detection> detectBarcodes(ImageData imageData);
}
