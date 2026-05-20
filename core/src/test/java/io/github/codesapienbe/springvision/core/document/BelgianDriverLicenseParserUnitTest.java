package io.github.codesapienbe.springvision.core.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class BelgianDriverLicenseParserUnitTest {

    private final BelgianDriverLicenseParser parser = new BelgianDriverLicenseParser();

    @Test
    void returnsEmptyWithoutMarkers() {
        assertThat(parser.tryParse("hello world")).isEmpty();
        assertThat(parser.tryParse(null)).isEmpty();
    }

    @Test
    void parsesValidBelgianDriverLicense() {
        String text =
            "BELGIQUE BELGIE BELGIEN\n"
            + "PERMIS DE CONDUIRE / RIJBEWIJS\n"
            + "1. TESTOV\n"
            + "2. ANNA\n"
            + "3. 30.07.1985 BRUSSELS\n"
            + "4a. 15.03.2018\n"
            + "4b. 15.03.2028\n"
            + "4c. SPF MOB.\n"
            + "5. ABCD123456\n"
            + "9. B BE\n";

        Optional<ParsedDocument> parsed = parser.tryParse(text);
        assertThat(parsed).isPresent();
        ParsedDocument doc = parsed.get();
        assertThat(doc.type()).isEqualTo(DocumentType.BELGIAN_DRIVER_LICENSE);
        assertThat(doc.field("surname")).isEqualTo("TESTOV");
        assertThat(doc.field("givenNames")).isEqualTo("ANNA");
        assertThat(doc.field("dateOfBirth")).isEqualTo("1985-07-30");
        assertThat(doc.field("placeOfBirth")).isEqualTo("BRUSSELS");
        assertThat(doc.field("issueDate")).isEqualTo("2018-03-15");
        assertThat(doc.field("expiryDate")).isEqualTo("2028-03-15");
        assertThat(doc.field("documentNumber")).isEqualTo("ABCD123456");
        assertThat(doc.field("categories")).isEqualTo("B BE");
        assertThat(doc.isValid()).isTrue();
    }

    @Test
    void invalidLicenseNumberFlagsValidationError() {
        String text =
            "BELGIQUE\n"
            + "RIJBEWIJS\n"
            + "1. TESTOV\n"
            + "2. ANNA\n"
            + "3. 30.07.1985 BRUSSELS\n"
            + "4a. 15.03.2018\n"
            + "4b. 15.03.2028\n"
            + "4c. SPF MOB.\n"
            + "5. ABCD12345\n"  // 9 chars — should fail regex
            + "9. B\n";

        ParsedDocument doc = parser.tryParse(text).orElseThrow();
        assertThat(doc.isValid()).isFalse();
        assertThat(doc.validationErrors())
            .anyMatch(s -> s.contains("documentNumber"));
    }

    @Test
    void issueAfterExpiryRaisesValidationError() {
        String text =
            "BELGIQUE\n"
            + "PERMIS DE CONDUIRE\n"
            + "1. TESTOV\n"
            + "2. ANNA\n"
            + "3. 30.07.1985 BRUSSELS\n"
            + "4a. 15.03.2030\n"  // issue after expiry
            + "4b. 15.03.2028\n"
            + "4c. SPF MOB.\n"
            + "5. ABCD123456\n"
            + "9. B\n";

        ParsedDocument doc = parser.tryParse(text).orElseThrow();
        assertThat(doc.isValid()).isFalse();
        assertThat(doc.validationErrors())
            .anyMatch(s -> s.contains("issueDate is after expiryDate"));
    }
}
