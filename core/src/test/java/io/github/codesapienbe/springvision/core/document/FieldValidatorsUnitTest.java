package io.github.codesapienbe.springvision.core.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class FieldValidatorsUnitTest {

    @Test
    void belgianNationalRegisterNumberAcceptsCanonicalFormat() {
        assertThat(FieldValidators.BELGIAN_NATIONAL_REGISTER_NUMBER.matcher("85.07.30-033.59").matches()).isTrue();
        assertThat(FieldValidators.BELGIAN_NATIONAL_REGISTER_NUMBER.matcher("85.07.30-33.59").matches()).isFalse();
        assertThat(FieldValidators.BELGIAN_NATIONAL_REGISTER_NUMBER.matcher("850730-033.59").matches()).isFalse();
        assertThat(FieldValidators.BELGIAN_NATIONAL_REGISTER_NUMBER.matcher("85-07-30-033-59").matches()).isFalse();
    }

    @Test
    void belgianEidCardNumberAcceptsCanonicalFormat() {
        assertThat(FieldValidators.BELGIAN_EID_CARD_NUMBER.matcher("591-1234567-89").matches()).isTrue();
        assertThat(FieldValidators.BELGIAN_EID_CARD_NUMBER.matcher("59-1234567-89").matches()).isFalse();
        assertThat(FieldValidators.BELGIAN_EID_CARD_NUMBER.matcher("591-1234567-9").matches()).isFalse();
    }

    @Test
    void belgianDriverLicenseNumberMustBeTenAlphanumeric() {
        assertThat(FieldValidators.BELGIAN_DRIVER_LICENSE_NUMBER.matcher("ABCD123456").matches()).isTrue();
        assertThat(FieldValidators.BELGIAN_DRIVER_LICENSE_NUMBER.matcher("0123456789").matches()).isTrue();
        assertThat(FieldValidators.BELGIAN_DRIVER_LICENSE_NUMBER.matcher("ABCD12345").matches()).isFalse();
        assertThat(FieldValidators.BELGIAN_DRIVER_LICENSE_NUMBER.matcher("abcd123456").matches()).isFalse();
    }

    @Test
    void dutchDocumentNumberMustBeNineAlphanumeric() {
        assertThat(FieldValidators.DUTCH_DOCUMENT_NUMBER.matcher("ABC123456").matches()).isTrue();
        assertThat(FieldValidators.DUTCH_DOCUMENT_NUMBER.matcher("12345678").matches()).isFalse();
        assertThat(FieldValidators.DUTCH_DOCUMENT_NUMBER.matcher("abc123456").matches()).isFalse();
    }

    @Test
    void sexValidatorAcceptsMfx() {
        assertThat(FieldValidators.SEX.matcher("M").matches()).isTrue();
        assertThat(FieldValidators.SEX.matcher("F").matches()).isTrue();
        assertThat(FieldValidators.SEX.matcher("X").matches()).isTrue();
        assertThat(FieldValidators.SEX.matcher("V").matches()).isFalse();
        assertThat(FieldValidators.SEX.matcher("MF").matches()).isFalse();
    }

    @Test
    void isoDateValidatorChecksCalendarValidity() {
        assertThat(FieldValidators.isValidIsoDate("2026-02-29")).isFalse();   // not a leap year
        assertThat(FieldValidators.isValidIsoDate("2024-02-29")).isTrue();
        assertThat(FieldValidators.isValidIsoDate("1985-07-30")).isTrue();
        assertThat(FieldValidators.isValidIsoDate("1985-7-30")).isFalse();
        assertThat(FieldValidators.isValidIsoDate(null)).isFalse();
        assertThat(FieldValidators.isValidIsoDate("")).isFalse();
    }

    @Test
    void requiredFieldsAreRegisteredForSupportedTypes() {
        for (DocumentType t : new DocumentType[]{
            DocumentType.BELGIAN_EID,
            DocumentType.BELGIAN_DRIVER_LICENSE,
            DocumentType.DUTCH_EID,
            DocumentType.DUTCH_DRIVER_LICENSE
        }) {
            assertThat(FieldValidators.requiredFields(t)).isNotEmpty();
            // surname and documentNumber are required across all four supported types
            assertThat(FieldValidators.requiredFields(t)).containsKey("surname");
            assertThat(FieldValidators.requiredFields(t)).containsKey("documentNumber");
        }
        assertThat(FieldValidators.requiredFields(DocumentType.LUX_EID)).isEmpty();
        assertThat(FieldValidators.requiredFields(DocumentType.UNKNOWN)).isEmpty();
    }

    @Test
    void requiredFieldRegexesAreUsablePatterns() {
        // Smoke test: every registered regex compiles and is usable on a string.
        for (DocumentType t : DocumentType.values()) {
            for (Pattern p : FieldValidators.requiredFields(t).values()) {
                assertThat(p.matcher("").matches()).isFalse();
            }
        }
    }
}
