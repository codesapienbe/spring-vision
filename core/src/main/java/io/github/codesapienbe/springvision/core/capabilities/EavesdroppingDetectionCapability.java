package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for detecting eavesdropping (shoulder surfing) attempts.
 *
 * <p>Backends implementing this interface analyze a temporal sequence of images
 * (video frames) to identify potential eavesdropping threats.</p>
 */
public interface EavesdroppingDetectionCapability {

    /**
     * Detects potential eavesdropping attempts from a sequence of images.
     */
    List<Detection> detectEavesdropping(List<ImageData> imageDataList);
}

