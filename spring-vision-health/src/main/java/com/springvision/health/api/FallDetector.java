package com.springvision.health.api;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;
import com.springvision.core.exception.BaseVisionException;

import java.util.List;

/**
 * Simplified fall detection API based on image sequences (no video sources).
 */
public interface FallDetector {

    /**
     * Detect falls from a temporal sequence of images.
     * Implementations should return a list of Detection objects describing events or per-frame findings.
     */
    List<Detection> detectFall(List<ImageData> imageDataList) throws BaseVisionException;
}
