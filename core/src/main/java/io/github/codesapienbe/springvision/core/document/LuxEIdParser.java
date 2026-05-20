package io.github.codesapienbe.springvision.core.document;

import java.util.Optional;

/**
 * Stub for Luxembourg identity card parsing.
 *
 * <p>Returns {@link Optional#empty()} pending full implementation in a follow-up.
 * Registered so {@link DocumentRegistry} has a complete Benelux roster.</p>
 */
public final class LuxEIdParser implements IdentityDocumentParser {

    @Override
    public DocumentType type() {
        return DocumentType.LUX_EID;
    }

    @Override
    public Optional<ParsedDocument> tryParse(String rawText) {
        return Optional.empty();
    }
}
