package io.github.codesapienbe.springvision.core.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DutchEIdParserUnitTest {

    private final DutchEIdParser parser = new DutchEIdParser();

    @Test
    void returnsEmptyForUnrelatedText() {
        assertThat(parser.tryParse("hello world")).isEmpty();
    }

    @Test
    void parsesValidDutchIdentityCardFromLabels() {
        String text =
            "KONINKRIJK DER NEDERLANDEN\n"
            + "IDENTITEITSKAART\n"
            + "Naam: DE VRIES\n"
            + "Voornamen: JAN PIETER\n"
            + "Geslacht: M\n"
            + "Geboortedatum: 30-07-1985\n"
            + "Geboorteplaats: AMSTERDAM\n"
            + "Geldig tot: 15-03-2028\n"
            + "Nationaliteit: NLD\n"
            + "Documentnummer: ABC123456\n"
            + "BSN: 123456789\n";

        Optional<ParsedDocument> parsed = parser.tryParse(text);
        assertThat(parsed).isPresent();
        ParsedDocument doc = parsed.get();
        assertThat(doc.type()).isEqualTo(DocumentType.DUTCH_EID);
        assertThat(doc.field("surname")).isEqualTo("DE VRIES");
        assertThat(doc.field("givenNames")).isEqualTo("JAN PIETER");
        assertThat(doc.field("dateOfBirth")).isEqualTo("1985-07-30");
        assertThat(doc.field("expiryDate")).isEqualTo("2028-03-15");
        assertThat(doc.field("documentNumber")).isEqualTo("ABC123456");
        assertThat(doc.field("bsn")).isEqualTo("123456789");
        assertThat(doc.field("nationality")).isEqualTo("NLD");
        assertThat(doc.field("sex")).isEqualTo("M");
        assertThat(doc.isValid()).isTrue();
        assertThat(doc.isExpired()).isFalse();
    }

    @Test
    void normalisesDutchVrouwelijkSexToF() {
        String text =
            "KONINKRIJK DER NEDERLANDEN\n"
            + "IDENTITEITSKAART\n"
            + "Naam: JANSEN\n"
            + "Voornamen: ANNA\n"
            + "Geslacht: V\n"  // Dutch shorthand for "vrouwelijk"
            + "Geboortedatum: 30-07-1985\n"
            + "Geldig tot: 15-03-2028\n"
            + "Documentnummer: ABC123456\n";

        ParsedDocument doc = parser.tryParse(text).orElseThrow();
        assertThat(doc.field("sex")).isEqualTo("F");
    }
}
