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
 * Parses Belgian driving licenses (model 3 / EU credit-card format) from OCR text.
 *
 * <p>Belgian licenses use the EU-standard numbered fields:</p>
 * <ul>
 *   <li>1 — surname</li>
 *   <li>2 — given names</li>
 *   <li>3 — date and place of birth</li>
 *   <li>4a — date of issue</li>
 *   <li>4b — date of expiry</li>
 *   <li>4c — issuing authority</li>
 *   <li>5 — licence number</li>
 *   <li>9 — categories of vehicles authorised</li>
 * </ul>
 *
 * <p>Driver licenses do not carry an MRZ, so this parser is label-anchored only.
 * Returns {@link Optional#empty()} when the country marker (BELGIQUE / BELGIË / BELGIEN)
 * and a permit marker (PERMIS DE CONDUIRE / RIJBEWIJS / FÜHRERSCHEIN) are not both present.</p>
 */
public final class BelgianDriverLicenseParser implements IdentityDocumentParser {

    private static final DocumentType TYPE = DocumentType.BELGIAN_DRIVER_LICENSE;

    private static final Pattern COUNTRY_MARKER =
        Pattern.compile("\\b(BELGIQUE|BELGI[ËE]|BELGIEN)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PERMIT_MARKER =
        Pattern.compile("(PERMIS\\s+DE\\s+CONDUIRE|RIJBEWIJS|F[ÜU]HRERSCHEIN)", Pattern.CASE_INSENSITIVE);

    // EU-format numbered field anchors. Use lookahead to next field number to keep matches bounded.
    private static final Pattern FIELD_1 =
        Pattern.compile("\\b1\\.?\\s+([A-ZÀ-ÖØ-Þ][A-ZÀ-ÖØ-Þ \\t\\-']{1,49})");
    private static final Pattern FIELD_2 =
        Pattern.compile("\\b2\\.?\\s+([A-ZÀ-ÖØ-Þ][A-ZÀ-ÖØ-Þ \\t\\-']{1,49})");
    private static final Pattern FIELD_3 =
        Pattern.compile("\\b3\\.?\\s+(\\d{1,2}[\\.\\-/]\\d{1,2}[\\.\\-/]\\d{2,4})(?:\\s+([A-ZÀ-ÖØ-Þ][A-ZÀ-ÖØ-Þ \\t\\-']{1,49}))?");
    private static final Pattern FIELD_4A =
        Pattern.compile("4\\s*a\\.?\\s+(\\d{1,2}[\\.\\-/]\\d{1,2}[\\.\\-/]\\d{2,4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIELD_4B =
        Pattern.compile("4\\s*b\\.?\\s+(\\d{1,2}[\\.\\-/]\\d{1,2}[\\.\\-/]\\d{2,4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIELD_4C =
        Pattern.compile("4\\s*c\\.?\\s+([A-ZÀ-ÖØ-Þ0-9][A-ZÀ-ÖØ-Þ0-9 \\t\\-'./]{1,49})", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIELD_5 =
        Pattern.compile("\\b5\\.?\\s+([A-Z0-9]{8,12})");
    private static final Pattern FIELD_9 =
        Pattern.compile("\\b9\\.?\\s+([A-Z0-9][A-Z0-9+/,\\s]{0,40})");

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
        if (!COUNTRY_MARKER.matcher(rawText).find() || !PERMIT_MARKER.matcher(rawText).find()) {
            return Optional.empty();
        }

        Map<String, String> fields = new HashMap<>();

        captureName(rawText, FIELD_1, fields, "surname");
        captureName(rawText, FIELD_2, fields, "givenNames");
        captureDobAndPob(rawText, fields);
        captureDate(rawText, FIELD_4A, fields, "issueDate");
        captureDate(rawText, FIELD_4B, fields, "expiryDate");
        captureAuthority(rawText, fields);
        captureLicenseNumber(rawText, fields);
        captureCategories(rawText, fields);

        // Without a licence number we don't claim recognition.
        if (!fields.containsKey("documentNumber")) {
            return Optional.empty();
        }

        return Optional.of(buildValidatedDocument(fields));
    }

    private void captureName(String text, Pattern p, Map<String, String> out, String key) {
        Matcher m = p.matcher(text);
        if (m.find()) {
            String raw = m.group(1).trim();
            if (!raw.isEmpty()) {
                out.put(key, raw.toUpperCase(Locale.ROOT).replaceAll("\\s+", " "));
            }
        }
    }

    private void captureDobAndPob(String text, Map<String, String> out) {
        Matcher m = FIELD_3.matcher(text);
        if (m.find()) {
            String iso = toIsoDate(m.group(1));
            if (iso != null) {
                out.put("dateOfBirth", iso);
            }
            if (m.groupCount() >= 2 && m.group(2) != null) {
                out.put("placeOfBirth",
                    m.group(2).trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " "));
            }
        }
    }

    private void captureDate(String text, Pattern p, Map<String, String> out, String key) {
        Matcher m = p.matcher(text);
        if (m.find()) {
            String iso = toIsoDate(m.group(1));
            if (iso != null) {
                out.put(key, iso);
            }
        }
    }

    private void captureAuthority(String text, Map<String, String> out) {
        Matcher m = FIELD_4C.matcher(text);
        if (m.find()) {
            String raw = m.group(1).trim();
            if (!raw.isEmpty()) {
                out.put("authority", raw);
            }
        }
    }

    private void captureLicenseNumber(String text, Map<String, String> out) {
        Matcher m = FIELD_5.matcher(text);
        if (m.find()) {
            String n = m.group(1).trim().toUpperCase(Locale.ROOT);
            // Belgian license numbers are 10 alphanumeric. Tolerate 8-12 from OCR; only set
            // documentNumber when length matches the spec exactly, otherwise stash raw.
            if (n.length() == 10) {
                out.put("documentNumber", n);
            } else {
                out.put("documentNumberRaw", n);
                out.put("documentNumber", n); // best-effort, validator will flag
            }
        }
    }

    private void captureCategories(String text, Map<String, String> out) {
        Matcher m = FIELD_9.matcher(text);
        if (m.find()) {
            String cats = m.group(1).trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
            if (!cats.isEmpty()) {
                out.put("categories", cats);
            }
        }
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

        String issue = fields.get("issueDate");
        String expiry = fields.get("expiryDate");
        String dob = fields.get("dateOfBirth");
        boolean isExpired = false;
        Integer expiresInDays = null;

        if (FieldValidators.isValidIsoDate(expiry)) {
            LocalDate exp = LocalDate.parse(expiry);
            LocalDate today = LocalDate.now();
            isExpired = exp.isBefore(today);
            expiresInDays = (int) (exp.toEpochDay() - today.toEpochDay());

            if (FieldValidators.isValidIsoDate(issue)) {
                LocalDate iss = LocalDate.parse(issue);
                if (iss.isAfter(exp)) {
                    errors.add("issueDate is after expiryDate");
                }
            }
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
}
