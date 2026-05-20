package io.github.codesapienbe.springvision.core.document;

import java.util.Optional;

/**
 * Stub for Luxembourg driving license parsing.
 *
 * <p>Returns {@link Optional#empty()} pending full implementation in a follow-up.</p>
 */
public final class LuxDriverLicenseParser implements IdentityDocumentParser {

    @Override
    public DocumentType type() {
        return DocumentType.LUX_DRIVER_LICENSE;
    }

    @Override
    public Optional<ParsedDocument> tryParse(String rawText) {
        return Optional.empty();
    }
}
