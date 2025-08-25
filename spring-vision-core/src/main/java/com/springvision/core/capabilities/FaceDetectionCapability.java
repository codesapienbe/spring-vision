package com.springvision.core.capabilities;

import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.exception.BaseVisionException;

/**
 * Capability interface for face detection.
 *
 * <p>Backends implementing this interface explicitly advertise support for
 * face detection independent of the broader {@code VisionBackend} SPI.</p>
 */
public interface FaceDetectionCapability {

	/** Detects faces in the provided image. */
	VisionResult detectFaces(ImageData imageData) throws BaseVisionException;
} 