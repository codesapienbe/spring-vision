package com.springvision.core.capabilities;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for pose estimation.
 *
 * <p>Backends implementing this interface explicitly advertise support for
 * pose estimation independent of the broader {@code VisionBackend} SPI.</p>
 */
public interface PoseEstimationCapability {

	/** Detects poses in the provided image. */
	List<Detection> detectPoses(ImageData imageData);
} 