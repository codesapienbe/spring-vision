package io.github.codesapienbe.springvision.core.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DutchDriverLicenseParserUnitTest {

    private final DutchDriverLicenseParser parser = new DutchDriverLicenseParser();

    @Test
    void returnsEmptyWithoutMarkers() {
        assertThat(parser.tryParse("random words")).isEmpty();
    }

    @Test
    void parsesValidDutchRijbewijs() {
        String text =
            "KONINKRIJK DER NEDERLANDEN\n"
            + "RIJBEWIJS\n"
            + "1. DE VRIES\n"
            + "2. JAN PIETER\n"
            + "3. 30.07.1985 UTRECHT\n"
            + "4a. 15.03.2018\n"
            + "4b. 15.03.2028\n"
            + "4c. RDW\n"
            + "5. ABC123456\n"
            + "9. B AM\n";

        Optional<ParsedDocument> parsed = parser.tryParse(text);
        assertThat(parsed).isPresent();
        ParsedDocument doc = parsed.get();
        assertThat(doc.type()).isEqualTo(DocumentType.DUTCH_DRIVER_LICENSE);
        assertThat(doc.field("surname")).isEqualTo("DE VRIES");
        assertThat(doc.field("givenNames")).isEqualTo("JAN PIETER");
        assertThat(doc.field("dateOfBirth")).isEqualTo("1985-07-30");
        assertThat(doc.field("issueDate")).isEqualTo("2018-03-15");
        assertThat(doc.field("expiryDate")).isEqualTo("2028-03-15");
        assertThat(doc.field("documentNumber")).isEqualTo("ABC123456");
        assertThat(doc.field("authority")).isEqualTo("RDW");
        assertThat(doc.field("categories")).isEqualTo("B AM");
        assertThat(doc.isValid()).isTrue();
    }
}
