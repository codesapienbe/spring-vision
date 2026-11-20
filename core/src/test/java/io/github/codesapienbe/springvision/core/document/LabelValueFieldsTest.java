package io.github.codesapienbe.springvision.core.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.capabilities.OcrCapability;

class LabelValueFieldsTest {

    private static OcrCapability.TextDetection line(String text, double confidence, Integer page) {
        Map<String, Object> attributes = new HashMap<>();
        if (page != null) {
            attributes.put("page", page);
        }
        return new OcrCapability.TextDetection(text, confidence, null, attributes);
    }

    @Test
    @DisplayName("'Label: value' lines become fields with their page and confidence, in any language")
    void fields() {
        List<LabelValueFields.Field> fields = LabelValueFields.of(List.of(
            line("FACTUUR", 0.97, 1),
            line("Factuurnummer: 2026-0042", 0.95, 1),
            line("0.4 Vehicle category: M1", 0.91, 2),
            line("Kleur van het voertuig: Blauw", 0.55, 2)));

        assertThat(fields).containsExactly(
            new LabelValueFields.Field("Factuurnummer", "2026-0042", 1, 0.95, false),
            new LabelValueFields.Field("0.4 Vehicle category", "M1", 2, 0.91, false),
            new LabelValueFields.Field("Kleur van het voertuig", "Blauw", 2, 0.55, true));
    }

    @Test
    @DisplayName("times, URLs, lines without a label or a value, and sentences are not fields")
    void notFields() {
        assertThat(LabelValueFields.of(List.of(
            line("Opening at 10:30 every day", 1.0, null),
            line("See https://example.org/terms", 1.0, null),
            line(": orphan value", 1.0, null),
            line("Remarks:", 1.0, null),
            line("12: 1300 kg", 1.0, null),
            line("x".repeat(101) + ": too long to be a label", 1.0, null),
            line(null, 1.0, null)))).isEmpty();
    }

    @Test
    @DisplayName("a time before the label's colon does not hide it, and a line without a page has no page")
    void colonAfterTime() {
        assertThat(LabelValueFields.of(List.of(line("Pickup 10:30 location: Example Street 1", 0.9, null))))
            .containsExactly(new LabelValueFields.Field("Pickup 10:30 location", "Example Street 1", null, 0.9, false));
        assertThat(LabelValueFields.splittingColon("no colon here")).isEqualTo(-1);
    }

    @Test
    @DisplayName("a document's content gives its fields from its lines; tables keep their rows")
    void content() {
        DocumentContent content = new DocumentContent(List.of(line("Name: Example", 1.0, 1)),
            List.of(new DocumentContent.Table(null, List.of(List.of("a", "b")))), Map.of("plate", "1-ABC-123"),
            Map.of("pages", "1"));

        assertThat(content.fields()).extracting(LabelValueFields.Field::value).containsExactly("Example");
        assertThat(content.tables().getFirst().rows()).containsExactly(List.of("a", "b"));
        assertThat(DocumentContent.ofLines(List.of()).formFields()).isEmpty();
    }
}
