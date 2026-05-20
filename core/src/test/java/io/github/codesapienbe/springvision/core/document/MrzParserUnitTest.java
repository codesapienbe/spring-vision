package io.github.codesapienbe.springvision.core.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class MrzParserUnitTest {

    /**
     * Helper to compose a TD1 MRZ block from already-validated check digits.
     * (The known-good values below were computed against the ICAO 9303
     * algorithm and asserted via {@link MrzParser#computeCheckDigit(String)}.)
     */
    private static String td1(String l1, String l2, String l3) {
        return l1 + "\n" + l2 + "\n" + l3;
    }

    @Test
    void checkDigitMatchesIcaoSpec() {
        // ICAO 9303 Appendix A worked example.
        assertThat(MrzParser.computeCheckDigit("D23145890")).isEqualTo(7);
        assertThat(MrzParser.computeCheckDigit("340712")).isEqualTo(7);
    }

    @Test
    void parsesValidBelgianTd1() {
        // Composed and check-digit-validated by hand; see test fixtures comment.
        // Doc number 591123456 → check 8
        // DOB 850730 → check 9
        // Expiry 280315 → check 7
        // Composite over (l1[5..30) + l2[0..7) + l2[8..15) + l2[18..29)) → check 4
        String mrz = td1(
            "I<BEL5911234568<<<<<<<<<<<<<<<",
            "8507309M2803157BEL<<<<<<<<<<<4",
            "MUSTERMANN<<MAX<<<<<<<<<<<<<<<"
        );

        Optional<MrzParser.MrzResult> result = MrzParser.parseTd1(mrz);

        assertThat(result).isPresent();
        var fields = result.get().fields();
        assertThat(fields).containsEntry("issuingCountry", "BEL");
        assertThat(fields).containsEntry("documentNumber", "591123456");
        assertThat(fields).containsEntry("nationality", "BEL");
        assertThat(fields).containsEntry("sex", "M");
        assertThat(fields).containsEntry("dateOfBirth", "1985-07-30");
        assertThat(fields).containsEntry("expiryDate", "2028-03-15");
        assertThat(fields).containsEntry("surname", "MUSTERMANN");
        assertThat(fields).containsEntry("givenNames", "MAX");
    }

    @Test
    void rejectsWrongDocumentNumberCheckDigit() {
        // Last digit of doc-number check tampered: '8' → '0'
        String mrz = td1(
            "I<BEL5911234560<<<<<<<<<<<<<<<",
            "8507309M2803157BEL<<<<<<<<<<<4",
            "MUSTERMANN<<MAX<<<<<<<<<<<<<<<"
        );
        assertThat(MrzParser.parseTd1(mrz)).isEmpty();
    }

    @Test
    void rejectsWrongDobCheckDigit() {
        String mrz = td1(
            "I<BEL5911234568<<<<<<<<<<<<<<<",
            "8507300M2803157BEL<<<<<<<<<<<4",
            "MUSTERMANN<<MAX<<<<<<<<<<<<<<<"
        );
        assertThat(MrzParser.parseTd1(mrz)).isEmpty();
    }

    @Test
    void rejectsWrongCompositeCheckDigit() {
        String mrz = td1(
            "I<BEL5911234568<<<<<<<<<<<<<<<",
            "8507309M2803157BEL<<<<<<<<<<<0",
            "MUSTERMANN<<MAX<<<<<<<<<<<<<<<"
        );
        assertThat(MrzParser.parseTd1(mrz)).isEmpty();
    }

    @Test
    void returnsEmptyForGarbageInput() {
        assertThat(MrzParser.parseTd1("hello world")).isEmpty();
        assertThat(MrzParser.parseTd1(null)).isEmpty();
        assertThat(MrzParser.parseTd1("")).isEmpty();
    }

    @Test
    void ignoresNoiseLinesAndStillParses() {
        String mrz = "Some header line\n"
            + "more noise above the MRZ block\n"
            + "I<BEL5911234568<<<<<<<<<<<<<<<\n"
            + "8507309M2803157BEL<<<<<<<<<<<4\n"
            + "MUSTERMANN<<MAX<<<<<<<<<<<<<<<\n"
            + "trailing noise\n";

        Optional<MrzParser.MrzResult> result = MrzParser.parseTd1(mrz);
        assertThat(result).isPresent();
        assertThat(result.get().fields().get("surname")).isEqualTo("MUSTERMANN");
    }
}
