package com.springvision.core.capabilities;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;
import com.springvision.core.exception.BaseVisionException;

import java.util.function.Predicate;

/** Capability interface for image obscuring and annotation operations. */
public interface AnnotationCapability {

	/** Obscures detections matching the filter (e.g., blur faces). */
	ImageData obscure(ImageData imageData, Predicate<Detection> filter) throws BaseVisionException;

	/** Performs generic annotation (e.g., draw boxes/labels) specified by request. */
	ImageData annotate(ImageData imageData, Object annotationRequest) throws BaseVisionException;
} 