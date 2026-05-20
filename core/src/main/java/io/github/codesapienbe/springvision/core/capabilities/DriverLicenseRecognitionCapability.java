package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Capability for recognizing driving licenses and extracting structured fields.
 *
 * <p>Returns a single Detection per recognized document. All structured fields
 * (surname, given names, license number, issue / expiry dates, license categories,
 * issuing authority, etc.) are placed in the Detection's {@code attributes} map.</p>
 *
 * <p>Supported documents (initial scope, Benelux):</p>
 * <ul>
 *   <li>{@code BELGIAN_DRIVER_LICENSE} — Belgian driving license (model 3 / 2013–2023)</li>
 *   <li>{@code DUTCH_DRIVER_LICENSE} — Dutch RDW driving license</li>
 * </ul>
 *
 * <p>Per the project's "no data is better than wrong data" rule, implementations
 * MUST throw {@link io.github.codesapienbe.springvision.core.exception.VisionProcessingException}
 * when the input cannot be confidently recognized as a supported driving license.</p>
 */
public interface DriverLicenseRecognitionCapability {

    /**
     * Recognizes a driving license in the given image and extracts its fields.
     *
     * @param imageData the image data to process
     * @param countryHint optional ISO 3166-1 alpha-2 hint ("BE", "NL", "LU"); {@code null}
     *                    asks the registry to auto-detect from language anchors
     * @return a list containing a single Detection with parsed fields in attributes
     * @throws BaseVisionException if the document cannot be recognized or processing fails
     */
    List<Detection> recognizeDriverLicense(ImageData imageData, String countryHint) throws BaseVisionException;

    /**
     * Reports whether driver license recognition is available on this backend.
     *
     * @return true if recognition is operational
     */
    boolean isDriverLicenseRecognitionAvailable();
}
