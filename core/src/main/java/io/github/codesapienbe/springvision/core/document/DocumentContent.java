package io.github.codesapienbe.springvision.core.document;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.codesapienbe.springvision.core.capabilities.OcrCapability;

/**
 * What was read from a document or an image, for any kind of document: no layout is assumed.
 *
 * <p>{@code lines} is the text, one line each, with its page, source and confidence.
 * {@link #fields()} are the lines of the form "Label: value", derived from {@code lines}.
 * {@code tables}, {@code formFields} and {@code metadata} are what the file itself declares, so
 * they are empty for an image and for a scanned page.</p>
 *
 * @param lines the text lines in reading order
 * @param tables the tables the file declares, each as rows of cells
 * @param formFields the filled-in fields of a PDF form, by field name, in form order
 * @param metadata the document's own properties (title, author, created, modified, pages, …)
 */
public record DocumentContent(
    List<OcrCapability.TextDetection> lines,
    List<Table> tables,
    Map<String, String> formFields,
    Map<String, String> metadata
) {

    public DocumentContent {
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(tables, "tables");
        Objects.requireNonNull(formFields, "formFields");
        Objects.requireNonNull(metadata, "metadata");
        lines = List.copyOf(lines);
        tables = List.copyOf(tables);
        formFields = Collections.unmodifiableMap(new LinkedHashMap<>(formFields));
        metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    /** Text lines only, as read from an image or by OCR. */
    public static DocumentContent ofLines(List<OcrCapability.TextDetection> lines) {
        return new DocumentContent(lines, List.of(), Map.of(), Map.of());
    }

    /** The "Label: value" lines of the text, in reading order. */
    public List<LabelValueFields.Field> fields() {
        return LabelValueFields.of(lines);
    }

    /**
     * A table as the file declares it.
     *
     * @param page the page it is on, or {@code null} when the format has no pages (Word, Excel)
     * @param rows the rows, each a list of cell texts
     */
    public record Table(Integer page, List<List<String>> rows) {

        public Table {
            Objects.requireNonNull(rows, "rows");
            rows = rows.stream().map(List::copyOf).toList();
        }
    }
}
