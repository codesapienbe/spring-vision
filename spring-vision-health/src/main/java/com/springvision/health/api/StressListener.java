package com.springvision.health.api;

import com.springvision.core.health.StressSample;

/**
 * Listener for stress analysis results.
 */
public interface StressListener {

    /**
     * Called when a new stress sample is available.
     */
    void onSample(StressSample sample);

    /**
     * Called when the analysis session completes successfully.
     */
    void onComplete();

    /**
     * Called on errors during analysis.
     */
    void onError(Throwable t);
}
