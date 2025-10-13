package io.github.codesapienbe.springvision.health.api;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;
import java.util.Optional;

/**
 * Defines the contract for stress analysis systems that estimate stress levels
 * from a temporal sequence of images.
 * <p>
 * This interface is intended for backends capable of analyzing physiological signals
 * that can be remotely measured from facial video, such as heart rate variability (HRV)
 * derived from remote photoplethysmography (rPPG). Implementations will process a
 * series of images to infer stress-related metrics.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
public interface StressAnalyzer {

    /**
     * Analyzes a temporal sequence of images to estimate the subject's stress level.
     * <p>
     * Implementations should process the list of {@link ImageData} frames to extract
     * physiological signals and compute a stress score. The result is returned as a
     * list of {@link Detection} objects, which may contain a single aggregated result
     * or per-frame analysis. The detection's attributes map can be used to store
     * detailed metrics like stress score, confidence, HRV, and other relevant data.
     *
     * @param imageDataList A time-ordered list of {@link ImageData} frames to be analyzed.
     * @return A list of {@link Detection} objects containing stress analysis results.
     *         If no reliable analysis can be made, the list will be empty.
     * @throws BaseVisionException if an error occurs during the analysis process.
     */
    List<Detection> detectStress(List<ImageData> imageDataList) throws BaseVisionException;

    /**
     * Returns the preferred time window for analysis as an optional {@link java.time.Duration}.
     * <p>
     * Some stress analysis algorithms require a minimum duration of video to produce
     * a reliable reading (e.g., 10 seconds). This method provides a hint to callers
     * about the ideal length of the image sequence to provide to {@link #detectStress}.
     *
     * @return An {@link Optional} containing the preferred analysis duration, or an
     *         empty optional if there is no specific preference.
     */
    Optional<java.time.Duration> preferredWindow();
}
