package com.springvision.core.capabilities;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for stress analysis backends.
 * Simplified to accept a list of images and return detections.
 */
public interface StressAnalysisCapability {

    /**
     * Detect stress estimates from a list of images representing a temporal sequence.
     * Returns a list of Detection objects describing aggregated stress scores or per-frame samples.
     */
    List<Detection> detectStress(List<ImageData> imageDataList);
}
