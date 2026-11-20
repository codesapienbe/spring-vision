package io.github.codesapienbe.springvision.core.document;

import java.util.List;
import java.util.Optional;

import io.github.codesapienbe.springvision.core.capabilities.OcrCapability;

/**
 * Finds the "Label: value" lines in a text, in any language and any kind of document.
 *
 * <p>A line is a field when it has a colon with a label before it and a value after it. A colon
 * between two digits ({@code 10:30}) or before {@code //} ({@code https://}) does not split a
 * line. Nothing is corrected or completed: the label and the value are the line's own text, and a
 * line read with an OCR confidence below {@link #LOW_CONFIDENCE} is marked as such so the caller
 * can treat its value with care.</p>
 */
public final class LabelValueFields {

    /** Lines read with a lower OCR confidence are marked {@link Field#lowConfidence()}. */
    public static final double LOW_CONFIDENCE = 0.60;

    /** A longer "label" is a sentence with a colon in it, not a label. */
    private static final int MAX_LABEL_LENGTH = 100;

    private LabelValueFields() {
    }

    /**
     * One "Label: value" line.
     *
     * @param label the text before the colon
     * @param value the text after the colon
     * @param page the page it is on, or {@code null} when the text has no pages
     * @param confidence the confidence of its line, 1.0 for a document's own text
     * @param lowConfidence whether OCR read the line below {@link #LOW_CONFIDENCE}
     */
    public record Field(String label, String value, Integer page, double confidence, boolean lowConfidence) {
    }

    /** The fields among the lines, in their order. */
    public static List<Field> of(List<OcrCapability.TextDetection> lines) {
        return lines.stream()
            .map(LabelValueFields::field)
            .flatMap(Optional::stream)
            .toList();
    }

    private static Optional<Field> field(OcrCapability.TextDetection line) {
        String text = line.text() == null ? "" : line.text();
        int colon = splittingColon(text);
        if (colon < 0) {
            return Optional.empty();
        }
        String label = text.substring(0, colon).strip();
        String value = text.substring(colon + 1).strip();
        if (label.isEmpty() || value.isEmpty() || label.length() > MAX_LABEL_LENGTH
            || label.chars().noneMatch(Character::isLetter)) {
            return Optional.empty();
        }
        Object page = line.attributes() == null ? null : line.attributes().get("page");
        return Optional.of(new Field(label, value, page instanceof Integer number ? number : null,
            line.confidence(), line.confidence() < LOW_CONFIDENCE));
    }

    /** The first colon that separates a label from a value, or -1. */
    static int splittingColon(String text) {
        for (int index = text.indexOf(':'); index >= 0; index = text.indexOf(':', index + 1)) {
            boolean betweenDigits = index > 0 && index + 1 < text.length()
                && Character.isDigit(text.charAt(index - 1)) && Character.isDigit(text.charAt(index + 1));
            boolean beforeSlashes = text.startsWith("//", index + 1);
            if (!betweenDigits && !beforeSlashes) {
                return index;
            }
        }
        return -1;
    }
}
