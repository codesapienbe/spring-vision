package io.github.codesapienbe.springvision.core.document;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates OCR output against Belgian licence-plate formats and returns the plate in its
 * canonical, dash-separated form. Nothing is corrected: a read that does not match a format is
 * rejected, never repaired into one (an {@code O} read as {@code 0} stays a mismatch).
 *
 * <p>Formats: the current series {@code 1-ABC-123} (one digit, three letters, three digits) and the
 * older {@code ABC-123} and {@code 123-ABC} series.</p>
 */
public final class BelgianPlateFormat {

    private static final List<Pattern> FORMATS = List.of(
        Pattern.compile("([0-9])-?([A-Z]{3})-?([0-9]{3})"),
        Pattern.compile("([A-Z]{3})-?([0-9]{3})"),
        Pattern.compile("([0-9]{3})-?([A-Z]{3})"));

    private BelgianPlateFormat() {
    }

    /** Upper-cased {@code raw} with everything except letters, digits and dashes removed. */
    public static String cleaned(String raw) {
        return raw == null ? "" : raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9-]", "");
    }

    /**
     * The canonical plate if {@code raw} is exactly one Belgian plate; a leading {@code B} (the
     * country code on the EU band, often read along with the plate) is ignored.
     */
    public static Optional<String> canonical(String raw) {
        String text = cleaned(raw);
        Optional<String> plate = match(text);
        if (plate.isEmpty() && text.startsWith("B")) {
            plate = match(text.substring(1).replaceFirst("^-", ""));
        }
        return plate;
    }

    private static Optional<String> match(String text) {
        for (Pattern format : FORMATS) {
            Matcher m = format.matcher(text);
            if (m.matches()) {
                StringBuilder canonical = new StringBuilder(m.group(1));
                for (int i = 2; i <= m.groupCount(); i++) {
                    canonical.append('-').append(m.group(i));
                }
                return Optional.of(canonical.toString());
            }
        }
        return Optional.empty();
    }
}
