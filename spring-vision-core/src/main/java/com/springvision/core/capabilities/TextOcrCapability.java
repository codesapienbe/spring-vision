package com.springvision.core.capabilities;

import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.exception.BaseVisionException;

/** Capability interface for OCR (text detection/recognition). */
public interface TextOcrCapability {
	VisionResult detectText(ImageData imageData) throws BaseVisionException;
} 