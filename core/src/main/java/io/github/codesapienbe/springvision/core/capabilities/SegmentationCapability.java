package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Capability interface for image segmentation (semantic and instance).
 *
 * <p>Backends implementing this interface can perform pixel-level classification
 * of images, providing either semantic segmentation (per-pixel class labels) or
 * instance segmentation (per-object masks).</p>
 *
 * @author Spring Vision Team
 * @since 1.0.5
 */
public interface SegmentationCapability {

    /**
     * Performs semantic segmentation on the provided image.
     * Each pixel is classified into a category without distinguishing between instances.
     *
     * @param imageData The image to segment.
     * @return VisionResult containing segmentation mask and class information.
     * @throws BaseVisionException if segmentation fails
     */
    VisionResult segmentSemantic(ImageData imageData) throws BaseVisionException;

    /**
     * Performs instance segmentation on the provided image.
     * Each object instance is detected and masked separately.
     *
     * @param imageData The image to segment.
     * @return VisionResult containing instance masks and detections.
     * @throws BaseVisionException if segmentation fails
     */
    VisionResult segmentInstances(ImageData imageData) throws BaseVisionException;
}

