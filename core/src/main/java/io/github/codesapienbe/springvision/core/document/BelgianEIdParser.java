package io.github.codesapienbe.springvision.core.document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Belgian electronic identity cards from OCR text.
 *
 * <p>Strategy:</p>
 * <ol>
 *   <li>Detect a TD1 MRZ block via {@link MrzParser}. If found and check digits pass,
 *       use it as the authoritative source (MRZ check digits make this far more
 *       reliable than label-anchored OCR).</li>
 *   <li>Augment with front-of-card fields that aren't encoded in the MRZ:
 *       the Belgian national register number, the printed card number, and the
 *       place of birth.</li>
 *   <li>If no MRZ is found, fall back to label-anchored extraction using
 *       multilingual labels (FR / NL / DE).</li>
 * </ol>
 *
 * <p>Returns {@link Optional#empty()} unless the text contains either a successful
 * MRZ with Belgian issuing country, or enough Belgian-eID markers (BELGIQUE /
 * BELGIË / BELGIEN combined with an identity-card phrase) to confidently identify
 * the layout. Per the project rule, half-extracted results are never returned.</p>
 */
public final class BelgianEIdParser implements IdentityDocumentParser {

    private static final DocumentType TYPE = DocumentType.BELGIAN_EID;

    // Belgian-eID markers — any one of these in combination with a country marker
    // is enough to commit to this layout.
    private static final Pattern COUNTRY_MARKER =
        Pattern.compile("\\b(BELGIQUE|BELGI[ËE]|BELGIEN|BEL)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CARD_TYPE_MARKER =
        Pattern.compile("(CARTE\\s+D[' ]IDENTIT[ÉE]|IDENTITEITSKAART|PERSONALAUSWEIS|EID)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SURNAME_LABEL =
        Pattern.compile("(?:NOM|NAAM|NAME)[:\\.\\s]+([A-ZÀ-ÖØ-Þ][A-ZÀ-ÖØ-Þ \\t\\-']{1,49})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern GIVEN_NAMES_LABEL =
        Pattern.compile("(?:PR[ÉE]NOMS?|VOORNAMEN?|VORNAMEN?)[:\\.\\s]+([A-ZÀ-ÖØ-Þ][A-ZÀ-ÖØ-Þ \\t\\-']{1,49})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NATIONALITY_LABEL =
        Pattern.compile("(?:NATIONALIT[ÉE]|NATIONALITEIT|STAATSANGEH[ÖO]RIGKEIT)[:\\.\\s]+([A-Z]{3})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DOB_LABEL =
        Pattern.compile("(?:DATE\\s+DE\\s+NAISSANCE|GEBOORTEDATUM|GEBURTSDATUM)[:\\.\\s]+(\\d{1,2}[\\.\\-/]\\d{1,2}[\\.\\-/]\\d{2,4})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern POB_LABEL =
        Pattern.compile("(?:LIEU\\s+DE\\s+NAISSANCE|GEBOORTEPLAATS|GEBURTSORT)[:\\.\\s]+([A-ZÀ-ÖØ-Þ][A-ZÀ-ÖØ-Þ \\t\\-']{1,49})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SEX_LABEL =
        Pattern.compile("(?:SEXE|GESLACHT|GESCHLECHT)[:\\.\\s]+([MFVX])",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPIRY_LABEL =
        Pattern.compile("(?:VALABLE\\s+JUSQU[' ]AU|GELDIG\\s+TOT|G[ÜU]LTIG\\s+BIS)[:\\.\\s]+(\\d{1,2}[\\.\\-/]\\d{1,2}[\\.\\-/]\\d{2,4})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern NATIONAL_REGISTER_NUMBER =
        Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{2}-\\d{3}\\.\\d{2})");
    private static final Pattern CARD_NUMBER =
        Pattern.compile("(\\d{3}-\\d{7}-\\d{2})");

    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("d.M.yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("d-M-yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT)
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
        boolean mrzIsBelgian = mrz.isPresent()
            && "BEL".equals(mrz.get().fields().get("issuingCountry"));
        boolean labelMarkersPresent =
            COUNTRY_MARKER.matcher(rawText).find()
                && CARD_TYPE_MARKER.matcher(rawText).find();

        if (!mrzIsBelgian && !labelMarkersPresent) {
            return Optional.empty();
        }

        Map<String, String> fields = new HashMap<>();
        if (mrz.isPresent()) {
            // Trust MRZ for personal data, but keep the doc-number under a distinct key
            // because the MRZ form ([A-Z0-9]{1,9}) differs from the printed card-number
            // format (\d{3}-\d{7}-\d{2}). The label-anchored CARD_NUMBER match below
            // fills "documentNumber" if the front of the card is also visible.
            Map<String, String> mrzFields = new HashMap<>(mrz.get().fields());
            String mrzDocNumber = mrzFields.remove("documentNumber");
            fields.putAll(mrzFields);
            if (mrzDocNumber != null) {
                fields.put("documentNumberMrz", mrzDocNumber);
            }
        }

        // Always augment with front-of-card extras.
        addIfFound(rawText, NATIONAL_REGISTER_NUMBER, fields, "nationalRegisterNumber");
        addIfFound(rawText, CARD_NUMBER, fields, "documentNumber");

        // Label-anchored fallbacks for fields that may be missing from MRZ.
        addFromLabel(rawText, SURNAME_LABEL, fields, "surname", BelgianEIdParser::normaliseName);
        addFromLabel(rawText, GIVEN_NAMES_LABEL, fields, "givenNames", BelgianEIdParser::normaliseName);
        addFromLabel(rawText, NATIONALITY_LABEL, fields, "nationality", s -> s.toUpperCase(Locale.ROOT));
        addFromLabel(rawText, POB_LABEL, fields, "placeOfBirth", BelgianEIdParser::normaliseName);
        addFromLabel(rawText, SEX_LABEL, fields, "sex", BelgianEIdParser::normaliseSex);
        addDateFromLabel(rawText, DOB_LABEL, fields, "dateOfBirth");
        addDateFromLabel(rawText, EXPIRY_LABEL, fields, "expiryDate");

        // Without any identifying number (printed card, NN, or MRZ-form), we don't have enough.
        if (!fields.containsKey("documentNumber")
                && !fields.containsKey("nationalRegisterNumber")
                && !fields.containsKey("documentNumberMrz")) {
            return Optional.empty();
        }

        // Default nationality when the layout markers clearly indicate Belgium.
        fields.putIfAbsent("nationality", "BEL");

        return Optional.of(buildValidatedDocument(fields));
    }

    private ParsedDocument buildValidatedDocument(Map<String, String> fields) {
        List<String> errors = new ArrayList<>();
        Map<String, Pattern> required = new LinkedHashMap<>(FieldValidators.requiredFields(TYPE));

        // When MRZ supplied the identifying number, the printed-card regex would
        // false-flag a clean MRZ-only back-of-card read. Swap in the MRZ-form
        // validator and drop the NN requirement (NN isn't encoded in TD1).
        boolean hasMrzDocNumber = fields.containsKey("documentNumberMrz");
        boolean hasPrintedDocNumber = fields.containsKey("documentNumber");
        if (hasMrzDocNumber && !hasPrintedDocNumber) {
            required.remove("documentNumber");
            required.remove("nationalRegisterNumber");
            required.put("documentNumberMrz", FieldValidators.MRZ_DOCUMENT_NUMBER);
        }

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

        // Date-order sanity: DOB <= expiry.
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

    private static void addIfFound(String text, Pattern p, Map<String, String> out, String key) {
        Matcher m = p.matcher(text);
        if (m.find()) {
            out.put(key, m.group(1));
        }
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
                // try next
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
            case "V" -> "F"; // Dutch "vrouwelijk"
            case "M", "F", "X" -> v;
            default -> v;
        };
    }
}
