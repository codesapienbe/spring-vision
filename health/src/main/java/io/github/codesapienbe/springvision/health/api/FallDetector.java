package io.github.codesapienbe.springvision.health.api;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;

/**
 * Defines the contract for fall detection systems that analyze a sequence of images.
 * <p>
 * This interface is designed for backends that can process a temporal series of images
 * to identify fall events, which is a critical capability in health monitoring and
 * elderly care applications. Implementations are expected to analyze posture, velocity,
 * and other contextual cues from the image sequence.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
public interface FallDetector {

    /**
     * Analyzes a temporal sequence of images to detect if a fall has occurred.
     * <p>
     * Implementations should process the list of {@link ImageData} frames in order,
     * looking for patterns indicative of a fall. The returned list of {@link Detection}
     * objects should describe the detected event, potentially including the bounding box
     * of the person who fell and a confidence score for the event.
     *
     * @param imageDataList A time-ordered list of {@link ImageData} frames to be analyzed.
     * @return A list of {@link Detection} objects. If no fall is detected, the list will be empty.
     *         If a fall is detected, the list will contain one or more detections describing the event.
     * @throws BaseVisionException if an error occurs during the detection process.
     */
    List<Detection> detectFall(List<ImageData> imageDataList) throws BaseVisionException;
}
