package com.springvision.core.capabilities;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for text OCR.
 *
 * <p>Backends implementing this interface explicitly advertise support for
 * text OCR independent of the broader {@code VisionBackend} SPI.</p>
 */
public interface TextOcrCapability {

	/** Detects text in the provided image. */
	List<Detection> detectText(ImageData imageData);
} 