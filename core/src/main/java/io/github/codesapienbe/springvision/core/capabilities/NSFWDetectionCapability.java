package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;

/**
 * Capability for detecting NSFW (Not Safe For Work) content in images.
 *
 * <p>This capability provides classification of images into safe/unsafe categories
 * for content moderation purposes.</p>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Content moderation for user-generated content</li>
 *   <li>Automated filtering of inappropriate images</li>
 *   <li>Compliance with content policies</li>
 *   <li>Child safety and parental controls</li>
 * </ul>
 *
 * <h2>Verified Models</h2>
 * <ul>
 *   <li><b>Falconsai/nsfw_image_detection</b> - Vision Transformer model with ~98% accuracy</li>
 *   <li><b>LAION-AI/CLIP-based-NSFW-Detector</b> - CLIP-based model outputting 0-1 score</li>
 * </ul>
 *
 * <h2>Detection Attributes</h2>
 * <p>The returned Detection contains:</p>
 * <ul>
 *   <li><b>isNSFW</b> (Boolean) - Whether content is NSFW</li>
 *   <li><b>classification</b> (String) - Classification label ("normal" or "nsfw")</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @see ImageClassificationCapability
 * @since 1.0.8
 */
public interface NSFWDetectionCapability {

    /**
     * Detects NSFW content in an image.
     *
     * <p>Returns a single Detection with label matching the classification ("normal" or "nsfw")
     * and confidence score. The detection attributes contain isNSFW boolean and classification string.</p>
     *
     * @param imageData the image to analyze
     * @return list with single detection containing NSFW classification
     * @throws BaseVisionException if detection fails
     */
    List<Detection> detectNSFW(ImageData imageData) throws BaseVisionException;
}

