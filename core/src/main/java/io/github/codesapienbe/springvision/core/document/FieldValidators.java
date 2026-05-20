package io.github.codesapienbe.springvision.core.document;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Centralised regex and date validators for identity-document fields.
 *
 * <p>Every regex used to declare a field valid lives here so tests can assert
 * against the same expressions the parsers use, and so adding a country only
 * requires extending these maps.</p>
 */
public final class FieldValidators {

    /** Belgian national register number, format {@code YY.MM.DD-XXX.XX}. */
    public static final Pattern BELGIAN_NATIONAL_REGISTER_NUMBER =
        Pattern.compile("^\\d{2}\\.\\d{2}\\.\\d{2}-\\d{3}\\.\\d{2}$");

    /** Belgian eID card number, format {@code XXX-XXXXXXX-XX} (12 digits). */
    public static final Pattern BELGIAN_EID_CARD_NUMBER =
        Pattern.compile("^\\d{3}-\\d{7}-\\d{2}$");

    /** Belgian driving licence number: 10 alphanumeric uppercase characters. */
    public static final Pattern BELGIAN_DRIVER_LICENSE_NUMBER =
        Pattern.compile("^[A-Z0-9]{10}$");

    /** Dutch document number (BSN-style 9 digits used on driver license and eID). */
    public static final Pattern DUTCH_DOCUMENT_NUMBER =
        Pattern.compile("^[A-Z0-9]{9}$");

    /** Dutch BSN (Burgerservicenummer), 8 or 9 digits. */
    public static final Pattern DUTCH_BSN = Pattern.compile("^\\d{8,9}$");

    /** ISO 8601 date format used as the normalised representation in {@link ParsedDocument#fields()}. */
    public static final Pattern ISO_DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    /** ICAO 9303 country / nationality code (3 uppercase letters). */
    public static final Pattern ICAO_COUNTRY = Pattern.compile("^[A-Z]{3}$");

    /** Sex marker M / F / X (ICAO 9303 also allows {@code <}). */
    public static final Pattern SEX = Pattern.compile("^[MFX]$");

    /** Required field keys per document type — every key listed must be present and match its validator. */
    private static final Map<DocumentType, Map<String, Pattern>> REQUIRED_FIELDS = Map.of(
        DocumentType.BELGIAN_EID, Map.of(
            "surname", Pattern.compile("^[A-Z][A-Z\\s\\-']{0,49}$"),
            "givenNames", Pattern.compile("^[A-Z][A-Z\\s\\-']{0,49}$"),
            "documentNumber", BELGIAN_EID_CARD_NUMBER,
            "nationalRegisterNumber", BELGIAN_NATIONAL_REGISTER_NUMBER,
            "dateOfBirth", ISO_DATE,
            "expiryDate", ISO_DATE,
            "nationality", ICAO_COUNTRY,
            "sex", SEX
        ),
        DocumentType.BELGIAN_DRIVER_LICENSE, Map.of(
            "surname", Pattern.compile("^[A-Z][A-Z\\s\\-']{0,49}$"),
            "givenNames", Pattern.compile("^[A-Z][A-Z\\s\\-']{0,49}$"),
            "documentNumber", BELGIAN_DRIVER_LICENSE_NUMBER,
            "dateOfBirth", ISO_DATE,
            "issueDate", ISO_DATE,
            "expiryDate", ISO_DATE,
            "categories", Pattern.compile("^[A-Z0-9+/, ]{1,40}$")
        ),
        DocumentType.DUTCH_EID, Map.of(
            "surname", Pattern.compile("^[A-Z][A-Z\\s\\-']{0,49}$"),
            "givenNames", Pattern.compile("^[A-Z][A-Z\\s\\-']{0,49}$"),
            "documentNumber", DUTCH_DOCUMENT_NUMBER,
            "dateOfBirth", ISO_DATE,
            "expiryDate", ISO_DATE,
            "nationality", ICAO_COUNTRY,
            "sex", SEX
        ),
        DocumentType.DUTCH_DRIVER_LICENSE, Map.of(
            "surname", Pattern.compile("^[A-Z][A-Z\\s\\-']{0,49}$"),
            "givenNames", Pattern.compile("^[A-Z][A-Z\\s\\-']{0,49}$"),
            "documentNumber", DUTCH_DOCUMENT_NUMBER,
            "dateOfBirth", ISO_DATE,
            "issueDate", ISO_DATE,
            "expiryDate", ISO_DATE,
            "categories", Pattern.compile("^[A-Z0-9+/, ]{1,40}$")
        )
    );

    private FieldValidators() {
        // utility
    }

    /**
     * Returns the required-field validator map for the given document type,
     * or an empty map if the type has no validators registered (e.g. {@link DocumentType#UNKNOWN}
     * or a stubbed Luxembourg layout).
     */
    public static Map<String, Pattern> requiredFields(DocumentType type) {
        return REQUIRED_FIELDS.getOrDefault(type, Map.of());
    }

    /**
     * Parses an ISO 8601 date string and returns {@code true} when it is a valid calendar date.
     */
    public static boolean isValidIsoDate(String s) {
        if (s == null || !ISO_DATE.matcher(s).matches()) {
            return false;
        }
        try {
            LocalDate.parse(s);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }
}
