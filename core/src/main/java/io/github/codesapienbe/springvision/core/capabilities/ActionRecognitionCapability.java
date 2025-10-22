package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Capability interface for action recognition from images or video frames.
 *
 * <p>Backends implementing this interface can identify human actions and activities
 * from visual input, such as walking, running, sitting, waving, etc.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.5
 */
public interface ActionRecognitionCapability {

    /**
     * Recognizes actions in the provided image.
     *
     * @param imageData The image to process for action recognition.
     * @return A list of detected actions with confidence scores.
     * @throws BaseVisionException if action recognition fails
     */
    List<Detection> recognizeActions(ImageData imageData) throws BaseVisionException;

    /**
     * Checks if action recognition models are available and loaded.
     *
     * @return true if action recognition models are available, false otherwise
     */
    boolean isActionRecognitionModelAvailable();
}
