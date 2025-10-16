package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;
import java.util.Map;

/**
 * Capability interface for Optical Character Recognition (OCR).
 *
 * <p>Backends implementing this interface can extract text from images.</p>
 */
public interface OcrCapability {

    /**
     * Extracts text from the provided image.
     *
     * @param imageData The image to process.
     * @return A list of detected text blocks with their locations and content.
     * @throws BaseVisionException if OCR processing fails
     */
    List<TextDetection> extractText(ImageData imageData) throws BaseVisionException;

    /**
     * Represents a detected text block in an image.
     */
    record TextDetection(
        String text,
        double confidence,
        Map<String, Object> boundingBox,
        Map<String, Object> attributes
    ) {
    }
}

