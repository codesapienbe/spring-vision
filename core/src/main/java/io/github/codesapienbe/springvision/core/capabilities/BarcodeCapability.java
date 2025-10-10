package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for barcode detection.
 *
 * <p>Backends implementing this interface explicitly advertise support for
 * barcode detection independent of the broader {@code VisionBackend} SPI.</p>
 */
public interface BarcodeCapability {

    /**
     * Detects barcodes in the provided image.
     */
    List<Detection> detectBarcodes(ImageData imageData);
}
