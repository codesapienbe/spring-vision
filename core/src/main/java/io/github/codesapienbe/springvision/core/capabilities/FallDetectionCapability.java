package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for fall detection backends.
 * Simplified to accept a list of images and return detections.
 */
public interface FallDetectionCapability {

    /**
     * Detect falls from a list of images representing a temporal sequence.
     * Returns a list of Detection objects describing events or per-frame findings.
     */
    List<Detection> detectFall(List<ImageData> imageDataList);
}
