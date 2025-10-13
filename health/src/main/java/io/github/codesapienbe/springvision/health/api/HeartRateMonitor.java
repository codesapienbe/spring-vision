package io.github.codesapienbe.springvision.health.api;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;

/**
 * Defines the contract for heart rate monitoring systems that estimate heart rate
 * from a temporal sequence of images.
 * <p>
 * This interface is designed for backends that use remote photoplethysmography (rPPG)
 * or other computer vision techniques to measure the subtle changes in skin color
 * caused by blood flow. Implementations are expected to be stateless and function
 * as pure as possible, processing a list of images to produce a result.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
public interface HeartRateMonitor {

    /**
     * Analyzes a temporal sequence of images to estimate the subject's heart rate.
     * <p>
     * Implementations should process the list of {@link ImageData} frames in order,
     * extracting the physiological signals needed to calculate beats per minute (BPM).
     * The result is returned as a list of {@link Detection} objects, which can be used
     * to encode aggregated metrics like minimum, maximum, and average BPM, along with
     * confidence scores, in the detection's metadata.
     *
     * @param imageDataList A time-ordered list of {@link ImageData} frames to be analyzed.
     * @return A list of {@link Detection} objects containing the heart rate analysis.
     *         If the analysis is inconclusive, the list will be empty.
     * @throws BaseVisionException if an error occurs during the analysis process.
     */
    List<Detection> detectHeartRate(List<ImageData> imageDataList) throws BaseVisionException;
}
