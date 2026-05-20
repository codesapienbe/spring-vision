package io.github.codesapienbe.springvision.core.document;

import java.util.Optional;

/**
 * Parses OCR text into a {@link ParsedDocument}.
 *
 * <p>Each implementation targets one specific document layout
 * (e.g. Belgian eID, Dutch driver license). The registry calls every parser
 * eligible for the {@link DocumentType#category() category} and picks the
 * one with the highest field-coverage score.</p>
 *
 * <p>Implementations MUST return {@link Optional#empty()} when the raw text
 * does not match their layout — they must not fall back to half-extracted fields.
 * Per the project rule "no data is better than wrong data".</p>
 */
public interface IdentityDocumentParser {

    /**
     * @return the document type this parser produces
     */
    DocumentType type();

    /**
     * Attempts to parse the raw OCR text.
     *
     * @param rawText OCR output (multi-line, may be noisy)
     * @return parsed document if the layout matches; {@link Optional#empty()} otherwise
     */
    Optional<ParsedDocument> tryParse(String rawText);
}
