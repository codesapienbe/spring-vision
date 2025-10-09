package com.springvision.core.health;

/**
 * Listener for fall detection events in core.
 */
public interface FallDetectionListener {
    void onFallDetected(com.springvision.core.health.FallEvent event);
    void onRecovery(com.springvision.core.health.FallEvent event);
    void onComplete();
    void onError(Throwable t);
}

