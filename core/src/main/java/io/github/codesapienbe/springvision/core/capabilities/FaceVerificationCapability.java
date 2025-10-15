package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Capability for verifying whether two face images belong to the same identity.
 */
public interface FaceVerificationCapability {

    /**
     * Verify whether two images belong to the same identity using embeddings.
     * Implementations must provide their own verification logic.
     */
    boolean verify(ImageData a, ImageData b, String metric, double threshold) throws BaseVisionException;

}

