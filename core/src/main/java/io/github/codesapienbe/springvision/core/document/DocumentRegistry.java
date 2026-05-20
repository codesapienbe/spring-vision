package io.github.codesapienbe.springvision.core.document;

import java.util.List;
import java.util.Optional;

/**
 * Registry of identity-document parsers.
 *
 * <p>Given OCR text plus an optional country hint, finds the best matching parser
 * by trying each one and picking the result with the most populated fields.
 * Parsers that don't recognise the layout return {@link Optional#empty()} and are
 * skipped. If no parser matches, the registry returns empty — callers must then
 * fail loudly per the project rule.</p>
 */
public final class DocumentRegistry {

    private static final List<IdentityDocumentParser> ID_CARD_PARSERS = List.of(
        new BelgianEIdParser(),
        new DutchEIdParser(),
        new LuxEIdParser()
    );

    private static final List<IdentityDocumentParser> DRIVER_LICENSE_PARSERS = List.of(
        new BelgianDriverLicenseParser(),
        new DutchDriverLicenseParser(),
        new LuxDriverLicenseParser()
    );

    private DocumentRegistry() {
    }

    /** Best-match identity-card parse for the given raw text. */
    public static Optional<ParsedDocument> parseIdentityCard(String rawText, String countryHint) {
        return best(ID_CARD_PARSERS, rawText, countryHint);
    }

    /** Best-match driver-license parse for the given raw text. */
    public static Optional<ParsedDocument> parseDriverLicense(String rawText, String countryHint) {
        return best(DRIVER_LICENSE_PARSERS, rawText, countryHint);
    }

    private static Optional<ParsedDocument> best(List<IdentityDocumentParser> parsers,
                                                  String rawText, String countryHint) {
        ParsedDocument winner = null;
        int winnerScore = -1;
        String normalisedHint = countryHint == null ? null : countryHint.trim().toUpperCase();

        for (IdentityDocumentParser parser : parsers) {
            if (normalisedHint != null
                && parser.type().countryCode() != null
                && !parser.type().countryCode().equals(normalisedHint)) {
                continue;
            }
            Optional<ParsedDocument> parsed = parser.tryParse(rawText);
            if (parsed.isEmpty()) {
                continue;
            }
            int score = score(parsed.get());
            if (score > winnerScore) {
                winner = parsed.get();
                winnerScore = score;
            }
        }
        return Optional.ofNullable(winner);
    }

    private static int score(ParsedDocument pd) {
        // Field count wins ties; a valid document outranks an invalid one with the same fields.
        return pd.fields().size() * 2 + (pd.isValid() ? 1 : 0);
    }
}
