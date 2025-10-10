package io.github.codesapienbe.springvision.core.health;

/**
 * Listener for fall detection events in core.
 */
public interface FallDetectionListener {
    void onFallDetected(io.github.codesapienbe.springvision.core.health.FallEvent event);

    void onRecovery(io.github.codesapienbe.springvision.core.health.FallEvent event);

    void onComplete();

    void onError(Throwable t);
}

