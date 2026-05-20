package io.github.codesapienbe.springvision.core.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class BelgianEIdParserUnitTest {

    private final BelgianEIdParser parser = new BelgianEIdParser();

    private static final String VALID_MRZ =
        "I<BEL5911234568<<<<<<<<<<<<<<<\n"
        + "8507309M2803157BEL<<<<<<<<<<<4\n"
        + "MUSTERMANN<<MAX<<<<<<<<<<<<<<<\n";

    @Test
    void returnsEmptyForUnrelatedText() {
        assertThat(parser.tryParse("hello world")).isEmpty();
        assertThat(parser.tryParse(null)).isEmpty();
    }

    @Test
    void parsesValidBelgianEidViaMrzPlusFrontFields() {
        String ocrText = VALID_MRZ
            + "BELGIQUE BELGIE BELGIEN\n"
            + "CARTE D'IDENTITE / IDENTITEITSKAART\n"
            + "591-1234567-89\n"
            + "N° NAT. 85.07.30-033.59\n";

        Optional<ParsedDocument> parsed = parser.tryParse(ocrText);

        assertThat(parsed).isPresent();
        ParsedDocument doc = parsed.get();
        assertThat(doc.type()).isEqualTo(DocumentType.BELGIAN_EID);
        assertThat(doc.fields().get("surname")).isEqualTo("MUSTERMANN");
        assertThat(doc.fields().get("givenNames")).isEqualTo("MAX");
        assertThat(doc.fields().get("nationality")).isEqualTo("BEL");
        assertThat(doc.fields().get("sex")).isEqualTo("M");
        assertThat(doc.fields().get("dateOfBirth")).isEqualTo("1985-07-30");
        assertThat(doc.fields().get("expiryDate")).isEqualTo("2028-03-15");
        assertThat(doc.fields().get("documentNumber")).isEqualTo("591-1234567-89");
        assertThat(doc.fields().get("nationalRegisterNumber")).isEqualTo("85.07.30-033.59");
    }

    @Test
    void flagsExpiredCardCorrectly() {
        // Card whose printed expiry is in the past.
        String text =
            "BELGIQUE BELGIE BELGIEN\n"
            + "CARTE D'IDENTITE\n"
            + "NOM: TESTOV\n"
            + "PRENOMS: ANNA\n"
            + "DATE DE NAISSANCE: 30.07.1985\n"
            + "VALABLE JUSQU'AU: 01.01.2020\n"
            + "SEXE: F\n"
            + "591-1234567-89\n"
            + "85.07.30-033.59\n";

        Optional<ParsedDocument> parsed = parser.tryParse(text);
        assertThat(parsed).isPresent();
        ParsedDocument doc = parsed.get();
        assertThat(doc.isExpired()).isTrue();
        assertThat(doc.field("expiryDate")).isEqualTo("2020-01-01");
        assertThat(doc.expiresInDays()).isNotNull();
        assertThat(doc.expiresInDays()).isNegative();
    }

    @Test
    void flagsFutureExpiryAsNotExpired() {
        LocalDate futureExpiry = LocalDate.now().plusYears(3);
        String text =
            "BELGIQUE\n"
            + "CARTE D'IDENTITE\n"
            + "NOM: TESTOV\n"
            + "PRENOMS: ANNA\n"
            + "DATE DE NAISSANCE: 30.07.1985\n"
            + "VALABLE JUSQU'AU: "
            + String.format("%02d.%02d.%04d",
                futureExpiry.getDayOfMonth(), futureExpiry.getMonthValue(), futureExpiry.getYear())
            + "\n"
            + "SEXE: F\n"
            + "591-1234567-89\n"
            + "85.07.30-033.59\n";

        ParsedDocument doc = parser.tryParse(text).orElseThrow();
        assertThat(doc.isExpired()).isFalse();
        assertThat(doc.expiresInDays()).isNotNull();
        assertThat(doc.expiresInDays()).isPositive();
    }

    @Test
    void invalidNationalRegisterNumberFlagsValidationError() {
        // NN with wrong shape — passes the loose capture but fails strict validation.
        String text =
            "BELGIQUE BELGIE\n"
            + "CARTE D'IDENTITE\n"
            + "NOM: TESTOV\n"
            + "PRENOMS: ANNA\n"
            + "DATE DE NAISSANCE: 30.07.1985\n"
            + "VALABLE JUSQU'AU: 15.03.2028\n"
            + "SEXE: F\n"
            + "591-1234567-89\n";
        // No NN at all here — parser must still recognise the card but flag the missing field.

        ParsedDocument doc = parser.tryParse(text).orElseThrow();
        assertThat(doc.isValid()).isFalse();
        assertThat(doc.validationErrors())
            .anyMatch(s -> s.contains("nationalRegisterNumber"));
    }
}
