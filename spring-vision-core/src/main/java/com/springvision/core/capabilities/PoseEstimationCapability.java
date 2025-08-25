package com.springvision.core.capabilities;

import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.exception.BaseVisionException;

/** Capability interface for human pose estimation. */
public interface PoseEstimationCapability {
	VisionResult detectPoses(ImageData imageData) throws BaseVisionException;
} 