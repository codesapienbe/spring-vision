package io.github.codesapienbe.springvision.core.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BelgianPlateFormat")
class BelgianPlateFormatTest {

    @Test
    @DisplayName("accepts the current and older series and canonicalises dashes")
    void acceptsKnownSeries() {
        assertThat(BelgianPlateFormat.canonical("1-ABC-123")).contains("1-ABC-123");
        assertThat(BelgianPlateFormat.canonical("1ABC123")).contains("1-ABC-123");
        assertThat(BelgianPlateFormat.canonical(" 2 xyz 789 ")).contains("2-XYZ-789");
        assertThat(BelgianPlateFormat.canonical("ABC-123")).contains("ABC-123");
        assertThat(BelgianPlateFormat.canonical("123ABC")).contains("123-ABC");
    }

    @Test
    @DisplayName("ignores a leading B read from the EU band")
    void ignoresEuBandLetter() {
        assertThat(BelgianPlateFormat.canonical("B1-ABC-123")).contains("1-ABC-123");
        assertThat(BelgianPlateFormat.canonical("B-1ABC123")).contains("1-ABC-123");
    }

    @Test
    @DisplayName("rejects reads that do not match, without repairing them")
    void rejectsWithoutRepair() {
        assertThat(BelgianPlateFormat.canonical("1-AB0-123")).isEmpty();
        assertThat(BelgianPlateFormat.canonical("1-ABC-12")).isEmpty();
        assertThat(BelgianPlateFormat.canonical("")).isEmpty();
        assertThat(BelgianPlateFormat.canonical(null)).isEmpty();
    }
}
