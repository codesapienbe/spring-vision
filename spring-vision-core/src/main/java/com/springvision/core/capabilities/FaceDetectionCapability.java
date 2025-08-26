package com.springvision.core.capabilities;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for face detection.
 *
 * <p>Backends implementing this interface explicitly advertise support for
 * face detection independent of the broader {@code VisionBackend} SPI.</p>
 */
public interface FaceDetectionCapability {

	/** Detects faces in the provided image. */
	List<Detection> detectFaces(ImageData imageData);
} 