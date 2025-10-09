package com.springvision.core.capabilities;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for heart-rate monitoring backends.
 * Simplified to accept a list of images as input and return detections.
 */
public interface HeartRateCapability {

    /**
     * Detect heart-rate related measurements from a list of images.
     * Returns a list of Detection objects where each detection can encode
     * aggregated information such as min/max/avg heart-rate or per-frame samples.
     */
    List<Detection> detectHeartRate(List<ImageData> imageDataList);
}
