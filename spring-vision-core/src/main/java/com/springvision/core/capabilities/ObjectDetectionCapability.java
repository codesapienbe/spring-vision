package com.springvision.core.capabilities;

import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.exception.BaseVisionException;

/** Capability interface for generic object detection. */
public interface ObjectDetectionCapability {
	VisionResult detectObjects(ImageData imageData) throws BaseVisionException;
} 