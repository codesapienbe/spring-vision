package com.springvision.core.capabilities;

import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.exception.BaseVisionException;

/** Capability interface for hand detection and gesture tracking. */
public interface HandDetectionCapability {
	VisionResult detectHands(ImageData imageData) throws BaseVisionException;
} 