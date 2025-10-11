package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for automated defect detection in manufacturing.
 *
 * <p>Backends implementing this interface analyze images from production lines
 * to identify defects in products such as scratches, dents, misalignments,
 * color variations, and other quality issues.</p>
 *
 * <p>The returned detections should include defect-specific attributes such as:
 * <ul>
 *   <li>defectType - type of defect (scratch, dent, crack, etc.)</li>
 *   <li>severity - severity level (minor, major, critical)</li>
 *   <li>location - precise location of the defect</li>
 *   <li>confidence - confidence score of the detection</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 2.0.0
 */
public interface DefectDetectionCapability {

    /**
     * Detects defects in products from a list of images.
     *
     * <p>Each returned detection represents a defect found in the product,
     * with attributes containing detailed defect information.</p>
     *
     * @param imageDataList list of product images to analyze
     * @return list of detections representing identified defects
     */
    List<Detection> detectDefects(List<ImageData> imageDataList);
}

