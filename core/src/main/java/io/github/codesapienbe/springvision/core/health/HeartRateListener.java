package io.github.codesapienbe.springvision.core.health;

/**
 * Core listener for heart rate samples emitted by backends.
 */
public interface HeartRateListener {
    void onSample(HeartRateResult sample);

    void onComplete();

    void onError(Throwable t);
}

