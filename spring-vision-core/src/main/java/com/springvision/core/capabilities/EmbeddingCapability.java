package com.springvision.core.capabilities;

import com.springvision.core.DetectionCategory;
import com.springvision.core.ImageData;
import com.springvision.core.exception.BaseVisionException;

import java.util.List;

/** Capability interface for embedding extraction (e.g., face embeddings). */
public interface EmbeddingCapability {
	List<float[]> extractEmbeddings(ImageData imageData, DetectionCategory subject) throws BaseVisionException;
} 