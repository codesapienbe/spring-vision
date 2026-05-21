package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Capability for detecting vehicle license plates and reading their text.
 *
 * <p>Implementations run a dedicated plate-detection model (typically a YOLO
 * single-class detector) to locate plate regions, then run OCR on each crop to
 * recover the plate string. Returning the bounding box and the raw OCR output
 * lets callers correlate the plate text with its location in the source frame.</p>
 *
 * <p>Each returned Detection carries:</p>
 * <ul>
 *   <li>{@code label} — the recognised plate text (whitespace-collapsed), or an
 *       empty string if OCR returned nothing for that crop.</li>
 *   <li>{@code confidence} — the detector's confidence in the bounding box.</li>
 *   <li>{@code boundingBox} — normalised plate coordinates in the source image.</li>
 *   <li>{@code attributes} — contains {@code "plateText"} (same as label),
 *       {@code "rawOcrText"} (Tesseract output before normalisation), and
 *       {@code "ocrConfidence"} (the OCR engine's per-crop confidence, when
 *       available).</li>
 * </ul>
 *
 * <p>Per the project's "no data is better than wrong data" rule, implementations
 * MUST throw {@link io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException}
 * when the plate-detection model is not bundled, rather than silently falling
 * back to whole-image OCR.</p>
 */
public interface LicensePlateRecognitionCapability {

    /**
     * Detects license plates in the given image and extracts the plate text.
     *
     * @param imageData the image to process
     * @return a list of Detection records, one per plate located by the detector
     *         (may be empty if no plate is found above the model's confidence
     *         threshold); never {@code null}
     * @throws BaseVisionException if the detector or OCR pipeline fails
     */
    List<Detection> recognizeLicensePlates(ImageData imageData) throws BaseVisionException;

    /**
     * Reports whether license-plate recognition is fully operational on this
     * backend (detection model loaded AND OCR engine available).
     *
     * @return true if both the plate-detection model and the OCR runtime are
     *         ready to serve requests
     */
    boolean isLicensePlateRecognitionAvailable();
}
