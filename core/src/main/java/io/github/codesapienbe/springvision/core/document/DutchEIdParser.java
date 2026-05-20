package io.github.codesapienbe.springvision.core.document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Dutch identity cards (Identiteitskaart) from OCR text.
 *
 * <p>Same two-stage strategy as {@link BelgianEIdParser}: MRZ first (TD1 on the
 * back of the card), then label-anchored fallback for front-of-card images.</p>
 */
public final class DutchEIdParser implements IdentityDocumentParser {

    private static final DocumentType TYPE = DocumentType.DUTCH_EID;

    private static final Pattern COUNTRY_MARKER =
        Pattern.compile("(NEDERLAND|KONINKRIJK\\s+DER\\s+NEDERLANDEN|NLD)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CARD_TYPE_MARKER =
        Pattern.compile("(IDENTITEITSKAART|IDENTITY\\s+CARD)", Pattern.CASE_INSENSITIVE);

    private static final Pattern SURNAME_LABEL =
        Pattern.compile("(?:NAAM|SURNAME)[:\\.\\s]+([A-ZÀ-ÖØ-Þ][A-ZÀ-ÖØ-Þ \\t\\-']{1,49})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern GIVEN_NAMES_LABEL =
        Pattern.compile("(?:VOORNAMEN?|GIVEN\\s+NAMES)[:\\.\\s]+([A-ZÀ-ÖØ-Þ][A-ZÀ-ÖØ-Þ \\t\\-']{1,49})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NATIONALITY_LABEL =
        Pattern.compile("(?:NATIONALITEIT|NATIONALITY)[:\\.\\s]+([A-Z]{3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOB_LABEL =
        Pattern.compile("(?:GEBOORTEDATUM|DATE\\s+OF\\s+BIRTH)[:\\.\\s]+(\\d{1,2}[\\.\\-/]\\d{1,2}[\\.\\-/]\\d{2,4})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern POB_LABEL =
        Pattern.compile("(?:GEBOORTEPLAATS|PLACE\\s+OF\\s+BIRTH)[:\\.\\s]+([A-ZÀ-ÖØ-Þ][A-ZÀ-ÖØ-Þ \\t\\-']{1,49})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SEX_LABEL =
        Pattern.compile("(?:GESLACHT|SEX)[:\\.\\s]+([MFVX])", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPIRY_LABEL =
        Pattern.compile("(?:GELDIG\\s+TOT|DATE\\s+OF\\s+EXPIRY)[:\\.\\s]+(\\d{1,2}[\\.\\-/]\\d{1,2}[\\.\\-/]\\d{2,4})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ISSUE_LABEL =
        Pattern.compile("(?:DATUM\\s+VAN\\s+AFGIFTE|DATE\\s+OF\\s+ISSUE)[:\\.\\s]+(\\d{1,2}[\\.\\-/]\\d{1,2}[\\.\\-/]\\d{2,4})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DOCUMENT_NUMBER_LABEL =
        Pattern.compile("(?:DOCUMENTNUMMER|DOCUMENT\\s+NUMBER)[:\\.\\s]+([A-Z0-9]{9})", Pattern.CASE_INSENSITIVE);
    private static final Pattern BSN_LABEL =
        Pattern.compile("(?:BSN|BURGERSERVICENUMMER)[:\\.\\s]+(\\d{8,9})", Pattern.CASE_INSENSITIVE);

    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("d.M.yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("d-M-yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)
    };

    @Override
    public DocumentType type() {
        return TYPE;
    }

    @Override
    public Optional<ParsedDocument> tryParse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }
        Optional<MrzParser.MrzResult> mrz = MrzParser.parseTd1(rawText);
        boolean mrzIsDutch = mrz.isPresent()
            && "NLD".equals(mrz.get().fields().get("issuingCountry"));
        boolean labelMarkersPresent =
            COUNTRY_MARKER.matcher(rawText).find()
                && CARD_TYPE_MARKER.matcher(rawText).find();

        if (!mrzIsDutch && !labelMarkersPresent) {
            return Optional.empty();
        }

        Map<String, String> fields = new HashMap<>();
        mrz.ifPresent(r -> fields.putAll(r.fields()));

        addFromLabel(rawText, SURNAME_LABEL, fields, "surname", DutchEIdParser::normaliseName);
        addFromLabel(rawText, GIVEN_NAMES_LABEL, fields, "givenNames", DutchEIdParser::normaliseName);
        addFromLabel(rawText, NATIONALITY_LABEL, fields, "nationality", s -> s.toUpperCase(Locale.ROOT));
        addFromLabel(rawText, POB_LABEL, fields, "placeOfBirth", DutchEIdParser::normaliseName);
        addFromLabel(rawText, SEX_LABEL, fields, "sex", DutchEIdParser::normaliseSex);
        addFromLabel(rawText, DOCUMENT_NUMBER_LABEL, fields, "documentNumber",
            s -> s.toUpperCase(Locale.ROOT));
        addFromLabel(rawText, BSN_LABEL, fields, "bsn", s -> s);
        addDateFromLabel(rawText, DOB_LABEL, fields, "dateOfBirth");
        addDateFromLabel(rawText, EXPIRY_LABEL, fields, "expiryDate");
        addDateFromLabel(rawText, ISSUE_LABEL, fields, "issueDate");

        if (!fields.containsKey("documentNumber") && !fields.containsKey("bsn")) {
            return Optional.empty();
        }

        fields.putIfAbsent("nationality", "NLD");

        return Optional.of(buildValidatedDocument(fields));
    }

    private ParsedDocument buildValidatedDocument(Map<String, String> fields) {
        List<String> errors = new ArrayList<>();
        Map<String, Pattern> required = FieldValidators.requiredFields(TYPE);
        for (Map.Entry<String, Pattern> req : required.entrySet()) {
            String key = req.getKey();
            String val = fields.get(key);
            if (val == null || val.isBlank()) {
                errors.add("missing field: " + key);
                continue;
            }
            if (!req.getValue().matcher(val).matches()) {
                errors.add("field does not match expected format: " + key + "=" + val);
            }
        }

        String dob = fields.get("dateOfBirth");
        String expiry = fields.get("expiryDate");
        boolean isExpired = false;
        Integer expiresInDays = null;
        if (FieldValidators.isValidIsoDate(expiry)) {
            LocalDate exp = LocalDate.parse(expiry);
            LocalDate today = LocalDate.now();
            isExpired = exp.isBefore(today);
            expiresInDays = (int) (exp.toEpochDay() - today.toEpochDay());
            if (FieldValidators.isValidIsoDate(dob)) {
                LocalDate dobDate = LocalDate.parse(dob);
                if (dobDate.isAfter(exp)) {
                    errors.add("dateOfBirth is after expiryDate");
                }
            }
        }

        boolean isValid = errors.isEmpty();
        return new ParsedDocument(TYPE, fields, errors, isExpired, isValid, expiresInDays);
    }

    private static void addFromLabel(String text, Pattern p, Map<String, String> out,
                                     String key, java.util.function.Function<String, String> normaliser) {
        if (out.containsKey(key)) {
            return;
        }
        Matcher m = p.matcher(text);
        if (m.find()) {
            String raw = m.group(1).trim();
            if (!raw.isEmpty()) {
                out.put(key, normaliser.apply(raw));
            }
        }
    }

    private static void addDateFromLabel(String text, Pattern p, Map<String, String> out, String key) {
        if (out.containsKey(key) && FieldValidators.isValidIsoDate(out.get(key))) {
            return;
        }
        Matcher m = p.matcher(text);
        if (m.find()) {
            String iso = toIsoDate(m.group(1));
            if (iso != null) {
                out.put(key, iso);
            }
        }
    }

    static String toIsoDate(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                LocalDate parsed = LocalDate.parse(trimmed, fmt);
                return parsed.toString();
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return null;
    }

    private static String normaliseName(String s) {
        return s.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static String normaliseSex(String s) {
        String v = s.toUpperCase(Locale.ROOT);
        return switch (v) {
            case "V" -> "F";
            case "M", "F", "X" -> v;
            default -> v;
        };
    }
}
