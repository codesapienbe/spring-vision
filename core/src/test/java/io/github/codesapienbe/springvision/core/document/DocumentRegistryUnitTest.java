package io.github.codesapienbe.springvision.core.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DocumentRegistryUnitTest {

    @Test
    void picksBelgianEidWhenBelgianMarkersPresent() {
        String text =
            "BELGIQUE BELGIE\n"
            + "CARTE D'IDENTITE\n"
            + "591-1234567-89\n"
            + "85.07.30-033.59\n";

        Optional<ParsedDocument> parsed = DocumentRegistry.parseIdentityCard(text, null);
        assertThat(parsed).isPresent();
        assertThat(parsed.get().type()).isEqualTo(DocumentType.BELGIAN_EID);
    }

    @Test
    void picksDutchEidWhenDutchMarkersPresent() {
        String text =
            "KONINKRIJK DER NEDERLANDEN\n"
            + "IDENTITEITSKAART\n"
            + "Documentnummer: ABC123456\n"
            + "BSN: 123456789\n";

        ParsedDocument doc = DocumentRegistry.parseIdentityCard(text, null).orElseThrow();
        assertThat(doc.type()).isEqualTo(DocumentType.DUTCH_EID);
    }

    @Test
    void countryHintRestrictsParserSet() {
        // Same text would match both BE and NL parsers, but a "NL" hint must filter out Belgian.
        String mixed =
            "BELGIQUE BELGIE NEDERLAND\n"
            + "IDENTITEITSKAART CARTE D'IDENTITE\n"
            + "591-1234567-89\n"
            + "Documentnummer: ABC123456\n"
            + "BSN: 123456789\n";

        ParsedDocument hinted = DocumentRegistry.parseIdentityCard(mixed, "NL").orElseThrow();
        assertThat(hinted.type()).isEqualTo(DocumentType.DUTCH_EID);

        ParsedDocument heBe = DocumentRegistry.parseIdentityCard(mixed, "BE").orElseThrow();
        assertThat(heBe.type()).isEqualTo(DocumentType.BELGIAN_EID);
    }

    @Test
    void luxStubsReturnEmpty() {
        assertThat(new LuxEIdParser().tryParse("any text")).isEmpty();
        assertThat(new LuxDriverLicenseParser().tryParse("any text")).isEmpty();
    }

    @Test
    void unrecognisedTextReturnsEmpty() {
        assertThat(DocumentRegistry.parseIdentityCard("definitely not an ID card", null)).isEmpty();
        assertThat(DocumentRegistry.parseDriverLicense("definitely not a license", null)).isEmpty();
    }
}
