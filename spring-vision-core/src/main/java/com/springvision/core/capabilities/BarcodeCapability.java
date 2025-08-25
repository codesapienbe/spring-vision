package com.springvision.core.capabilities;

import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.exception.BaseVisionException;

/** Capability interface for barcode and QR code detection/decoding. */
public interface BarcodeCapability {
	VisionResult detectBarcodes(ImageData imageData) throws BaseVisionException;
} 