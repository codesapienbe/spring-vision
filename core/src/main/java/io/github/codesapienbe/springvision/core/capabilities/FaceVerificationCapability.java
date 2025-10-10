package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Capability for verifying whether two face images belong to the same identity.
 * <p>
 * Default implementation delegates to core EmbeddingSupport utilities.
 */
public interface FaceVerificationCapability {

    default boolean verify(ImageData a, ImageData b, String metric, double threshold) throws BaseVisionException {
        return io.github.codesapienbe.springvision.core.util.EmbeddingSupport.defaultVerify(a, b, metric, threshold);
    }
}

