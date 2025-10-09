package com.springvision.health.api;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;
import com.springvision.core.exception.BaseVisionException;

import java.util.List;
import java.util.Optional;

/**
 * Simplified stress analysis API: accepts a list of images and returns detections.
 */
public interface StressAnalyzer {

    /**
     * Analyze a temporal sequence of images and return stress-related detections.
     * Each Detection may encode an aggregated stress score (min/max/avg) and confidence in its metadata.
     */
    List<Detection> detectStress(List<ImageData> imageDataList) throws BaseVisionException;

    /**
     * Optional: preferred analysis window (e.g. 10s) used as a hint by callers.
     */
    Optional<java.time.Duration> preferredWindow();
}
