package io.github.codesapienbe.springvision.core.document;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of parsing an identity document from OCR text.
 *
 * <p>{@code fields} is a string-to-string map whose keys are stable, language-neutral
 * identifiers (e.g. {@code surname}, {@code givenNames}, {@code documentNumber},
 * {@code dateOfBirth}, {@code expiryDate}). Dates are normalised to ISO 8601
 * ({@code yyyy-MM-dd}) when parsed successfully.</p>
 *
 * <p>{@code isValid} reflects field-format correctness only (every required field
 * present and matching its validator regex, date ordering is sane). {@code isExpired}
 * is a separate, time-dependent flag. A card may be {@code isValid=true} and
 * {@code isExpired=true} at the same time.</p>
 *
 * @param type the recognised document type
 * @param fields stable-keyed field map (immutable copy taken at construction)
 * @param validationErrors human-readable validation problems (empty when {@code isValid})
 * @param isExpired true iff {@code expiryDate} is present and strictly before today
 * @param isValid true iff all required fields are present and match their regex
 * @param expiresInDays signed days until expiry ({@code null} if no expiry date parsed)
 */
public record ParsedDocument(
    DocumentType type,
    Map<String, String> fields,
    List<String> validationErrors,
    boolean isExpired,
    boolean isValid,
    Integer expiresInDays
) {

    public ParsedDocument {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(fields, "fields");
        Objects.requireNonNull(validationErrors, "validationErrors");
        fields = Map.copyOf(fields);
        validationErrors = List.copyOf(validationErrors);
    }

    /** Returns the field value for the given key, or {@code null}. */
    public String field(String key) {
        return fields.get(key);
    }
}
