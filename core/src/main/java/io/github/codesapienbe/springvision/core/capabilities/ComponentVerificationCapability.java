package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Capability interface for component verification during assembly.
 *
 * <p>Backends implementing this interface analyze images to verify that the correct
 * components are being used during assembly processes. This includes checking
 * component type, part numbers, orientation, and placement correctness.</p>
 *
 * <p>The returned detections should include verification-specific attributes such as:
 * <ul>
 *   <li>componentId - identifier of the detected component</li>
 *   <li>partNumber - part number or model number</li>
 *   <li>verified - boolean indicating if component is correct</li>
 *   <li>expectedComponent - what component was expected (if verification failed)</li>
 *   <li>confidence - confidence score of the verification</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 2.0.0
 */
public interface ComponentVerificationCapability {

    /**
     * Verifies components in assembly operations from a list of images.
     *
     * <p>Each returned detection represents a component with verification results
     * indicating whether it's the correct component for the assembly step.</p>
     *
     * @param imageDataList list of images showing components to verify
     * @return list of detections with component verification results
     */
    List<Detection> verifyComponents(List<ImageData> imageDataList);
}
