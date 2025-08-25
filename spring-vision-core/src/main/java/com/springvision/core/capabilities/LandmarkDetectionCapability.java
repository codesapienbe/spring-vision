package com.springvision.core.capabilities;

import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.exception.BaseVisionException;

/** Capability interface for generic landmark detection. */
public interface LandmarkDetectionCapability {
	VisionResult detectLandmarks(ImageData imageData) throws BaseVisionException;
} 