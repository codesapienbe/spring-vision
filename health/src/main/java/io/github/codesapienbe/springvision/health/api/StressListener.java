package io.github.codesapienbe.springvision.health.api;

import io.github.codesapienbe.springvision.core.health.StressSample;

/**
 * Defines a listener for receiving asynchronous stress analysis results.
 * <p>
 * This interface is designed for applications that need to process stress analysis
 * data as it becomes available, rather than waiting for a complete analysis to finish.
 * It is particularly useful for real-time monitoring scenarios where immediate feedback
 * is required.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
public interface StressListener {

    /**
     * Called when a new stress sample is available from the analysis.
     * <p>
     * This method is invoked periodically by the stress analyzer, providing a
     * {@link StressSample} that contains the latest stress metrics. The frequency
     * of this callback depends on the implementation of the analyzer.
     *
     * @param sample The {@link StressSample} containing the latest stress data.
     */
    void onSample(StressSample sample);

    /**
     * Called when the stress analysis session completes successfully.
     * <p>
     * This method is invoked to signal that the analysis has finished and no more
     * samples will be sent. This could be triggered by the end of a video stream
     * or the successful processing of all provided data.
     */
    void onComplete();

    /**
     * Called if an unrecoverable error occurs during the stress analysis.
     * <p>
     * This method is invoked when the analyzer encounters a problem that prevents
     * it from continuing, such as an issue with the input data or an internal error.
     * After this method is called, no further calls to {@code onSample} or
     * {@code onComplete} will be made.
     *
     * @param t The {@link Throwable} that represents the error.
     */
    void onError(Throwable t);
}
