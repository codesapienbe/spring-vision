package io.github.codesapienbe.springvision.health.api;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

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
