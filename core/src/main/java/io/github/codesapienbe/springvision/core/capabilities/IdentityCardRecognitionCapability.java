package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Capability for recognizing national identity cards and extracting structured fields.
 *
 * <p>Returns a single Detection per recognized document. All structured fields
 * (surname, given names, document number, validity dates, etc.) are placed in the
 * Detection's {@code attributes} map together with {@code documentType},
 * {@code isValid}, {@code isExpired}, {@code expiresInDays}, {@code validationErrors}
 * and the raw OCR text.</p>
 *
 * <p>Supported documents (initial scope, Benelux):</p>
 * <ul>
 *   <li>{@code BELGIAN_EID} — Belgian electronic identity card (MRZ-first, label fallback)</li>
 *   <li>{@code DUTCH_EID} — Dutch identity card (MRZ-first, label fallback)</li>
 * </ul>
 *
 * <p>Per the project's "no data is better than wrong data" rule, implementations
 * MUST throw {@link io.github.codesapienbe.springvision.core.exception.VisionProcessingException}
 * when the input image cannot be confidently recognized as a supported identity
 * document — never return a partial result with fabricated fields.</p>
 */
public interface IdentityCardRecognitionCapability {

    /**
     * Recognizes a national identity card in the given image and extracts its fields.
     *
     * @param imageData the image data to process
     * @param countryHint optional ISO 3166-1 alpha-2 hint ("BE", "NL", "LU"); pass {@code null}
     *                    to let the registry auto-detect from MRZ and language anchors
     * @return a list containing a single Detection with parsed fields in attributes
     * @throws BaseVisionException if the document cannot be recognized or processing fails
     */
    List<Detection> recognizeIdentityCard(ImageData imageData, String countryHint) throws BaseVisionException;

    /**
     * Reports whether identity card recognition is available on this backend
     * (i.e. an OCR engine is reachable and required language packs are installed).
     *
     * @return true if recognition is operational
     */
    boolean isIdentityCardRecognitionAvailable();
}
