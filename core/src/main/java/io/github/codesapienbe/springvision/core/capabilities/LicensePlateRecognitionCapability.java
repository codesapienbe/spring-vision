package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Capability for detecting vehicle license plates and reading their text.
 *
 * <p>Implementations locate plate regions, run OCR on each crop and validate the text against
 * known plate formats. Returning the bounding box and the raw OCR output lets callers correlate
 * the plate text with its location in the source frame.</p>
 *
 * <p>Each returned Detection carries:</p>
 * <ul>
 *   <li>{@code label} — the plate in canonical form when it matches a known format, otherwise
 *       the cleaned OCR text. A read is never corrected into a format.</li>
 *   <li>{@code confidence} — the OCR engine's confidence for the read.</li>
 *   <li>{@code boundingBox} — normalised plate coordinates in the source image.</li>
 *   <li>{@code attributes} — {@code "plateText"} (canonical plate, or empty when the format
 *       did not match), {@code "rawOcrText"}, {@code "formatValid"}, and {@code "country"}
 *       when the format identifies one.</li>
 * </ul>
 *
 * <p>Per the project's "no data is better than wrong data" rule, implementations MUST throw
 * {@link io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException} when they
 * cannot read plates (for example missing OCR data), rather than return guesses.</p>
 */
public interface LicensePlateRecognitionCapability {

    /**
     * Detects license plates in the given image and extracts the plate text.
     *
     * @param imageData the image to process
     * @return a list of Detection records, one per plate read (may be empty); never {@code null}
     * @throws BaseVisionException if locating or reading plates fails
     */
    List<Detection> recognizeLicensePlates(ImageData imageData) throws BaseVisionException;

    /**
     * Reports whether license-plate recognition is operational on this backend.
     *
     * @return true if plates can be located and read
     */
    boolean isLicensePlateRecognitionAvailable();
}
