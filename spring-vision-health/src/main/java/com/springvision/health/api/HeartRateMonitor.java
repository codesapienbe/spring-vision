package com.springvision.health.api;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;
import com.springvision.core.exception.BaseVisionException;

import java.util.List;

/**
 * Simplified heart-rate detection API: accepts a list of images and returns detections.
 * Implementations should be stateless and pure functions where possible.
 */
public interface HeartRateMonitor {

    /**
     * Analyze a temporal sequence of images and return heart-rate related detections.
     * Typical usage: caller supplies consecutive frames; backend returns detections
     * encoding min/max/average bpm and confidence as Detection metadata.
     */
    List<Detection> detectHeartRate(List<ImageData> imageDataList) throws BaseVisionException;
}
