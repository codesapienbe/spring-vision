package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Capability interface for detecting eavesdropping (shoulder surfing) attempts.
 *
 * <p>Backends implementing this interface analyze a temporal sequence of images
 * (video frames) to identify potential eavesdropping threats.</p>
 */
public interface EavesdroppingDetectionCapability {

    /**
     * Detects potential eavesdropping attempts from a sequence of images.
     *
     * @param imageDataList the list of image frames to analyze
     * @return a list of detections describing potential eavesdropping events
     */
    List<Detection> detectEavesdropping(List<ImageData> imageDataList);
}
