package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.Map;

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
 * @author Spring Vision Team
 * @see ImageClassificationCapability
 * @since 1.0.8
 */
public interface NSFWDetectionCapability {

    /**
     * Detects NSFW content in an image.
     *
     * @param imageData the image to analyze
     * @return NSFW detection result with classification and confidence
     * @throws BaseVisionException if detection fails
     */
    NSFWResult detectNSFW(ImageData imageData) throws BaseVisionException;

    /**
     * Result of NSFW detection.
     *
     * @param isNSFW        whether the image contains NSFW content
     * @param confidence    confidence score (0.0 to 1.0)
     * @param classification the classification label (e.g., "normal", "nsfw")
     * @param attributes    additional attributes like model name, threshold used, etc.
     */
    record NSFWResult(
        boolean isNSFW,
        double confidence,
        String classification,
        Map<String, Object> attributes
    ) {
    }
}

