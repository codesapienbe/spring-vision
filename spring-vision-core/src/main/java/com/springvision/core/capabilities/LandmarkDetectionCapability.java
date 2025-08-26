package com.springvision.core.capabilities;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for landmark detection.
 *
 * <p>Backends implementing this interface explicitly advertise support for
 * landmark detection independent of the broader {@code VisionBackend} SPI.</p>
 */
public interface LandmarkDetectionCapability {

	/** Detects landmarks in the provided image. */
	List<Detection> detectLandmarks(ImageData imageData);
} 