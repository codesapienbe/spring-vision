package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for access authentication using vision (e.g., face recognition).
 *
 * <p>Backends implementing this interface verify access authorization from a single image
 * and return detection results describing the outcome (authorized/unauthorized) with
 * relevant metadata.</p>
 */
public interface AccessAuthenticationCapability {

    /**
     * Authenticates access from a single image.
     * @param imageData The image data to authenticate.
     * @return A list of detections with authentication results.
     */
    List<Detection> authenticateAccess(ImageData imageData);
}
