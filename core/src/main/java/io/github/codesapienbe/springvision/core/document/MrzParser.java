package io.github.codesapienbe.springvision.core.document;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ICAO 9303 Machine-Readable Zone parser for ID cards (TD1, 3×30 lines).
 *
 * <p>MRZ is far more reliable than label-anchored OCR: it uses a fixed
 * character set ({@code [A-Z0-9<]}), fixed field positions, and check digits
 * that let us reject corrupted reads. Belgian and Dutch eID cards both
 * carry a TD1 MRZ on the back of the card.</p>
 *
 * <p>TD1 layout (30 characters per line):</p>
 * <pre>
 * Line 1: I&lt;CCC&lt;DOC-NUMBER&lt;C&lt;OPTIONAL...........
 *         pos 1   2  3-5  6-14    15  16-30
 *         I = ID, CCC = issuing country, DOC-NUMBER = 9 chars, C = check digit
 *
 * Line 2: YYMMDD&lt;C&lt;S&lt;YYMMDD&lt;C&lt;NAT&lt;OPT.......C
 *         1-6   7 8  9-14  15 16-18 19-29     30
 *         DOB(6), check, sex, expiry(6), check, nationality(3), optional, composite-check
 *
 * Line 3: SURNAME&lt;&lt;GIVEN&lt;NAMES&lt;&lt;&lt;&lt;&lt;...........
 * </pre>
 */
public final class MrzParser {

    private static final int LINE_LEN = 30;
    private static final int[] WEIGHTS = {7, 3, 1};

    private MrzParser() {
    }

    /**
     * Attempts to detect and parse a TD1 MRZ block from raw OCR text.
     *
     * @return the parsed result, or empty when no plausible MRZ is found
     *         or any required check digit fails
     */
    public static Optional<MrzResult> parseTd1(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }
        List<String> lines = candidateLines(rawText);
        if (lines.size() < 3) {
            return Optional.empty();
        }

        // Find a triple where the first line starts with 'I' and all three lines are LINE_LEN long.
        for (int i = 0; i + 2 < lines.size(); i++) {
            String l1 = pad(lines.get(i));
            String l2 = pad(lines.get(i + 1));
            String l3 = pad(lines.get(i + 2));
            if (l1.charAt(0) != 'I') {
                continue;
            }
            Optional<MrzResult> parsed = tryParseTriple(l1, l2, l3);
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        return Optional.empty();
    }

    private static Optional<MrzResult> tryParseTriple(String l1, String l2, String l3) {
        String docCountry = l1.substring(2, 5);
        String docNumberRaw = l1.substring(5, 14);
        char docNumberCheck = l1.charAt(14);
        if (!checkDigitMatches(docNumberRaw, docNumberCheck)) {
            return Optional.empty();
        }

        String dobRaw = l2.substring(0, 6);
        char dobCheck = l2.charAt(6);
        if (!checkDigitMatches(dobRaw, dobCheck)) {
            return Optional.empty();
        }
        char sexChar = l2.charAt(7);
        String expiryRaw = l2.substring(8, 14);
        char expiryCheck = l2.charAt(14);
        if (!checkDigitMatches(expiryRaw, expiryCheck)) {
            return Optional.empty();
        }
        String nationality = l2.substring(15, 18);

        // Composite check: positions 1-4 + 6-30 of line 1, 1-7 + 9-15 + 19-29 of line 2.
        String composite =
            l1.substring(5, 30)
            + l2.substring(0, 7)
            + l2.substring(8, 15)
            + l2.substring(18, 29);
        char compositeCheck = l2.charAt(29);
        if (!checkDigitMatches(composite, compositeCheck)) {
            return Optional.empty();
        }

        String[] names = splitNames(l3);
        String surname = names[0];
        String givenNames = names[1];

        String dob = expandDate(dobRaw, /*expiry*/ false);
        String expiry = expandDate(expiryRaw, /*expiry*/ true);

        Map<String, String> fields = new HashMap<>();
        fields.put("documentNumber", normaliseDocNumber(docNumberRaw));
        fields.put("issuingCountry", docCountry);
        fields.put("nationality", nationality);
        fields.put("sex", normaliseSex(sexChar));
        if (dob != null) {
            fields.put("dateOfBirth", dob);
        }
        if (expiry != null) {
            fields.put("expiryDate", expiry);
        }
        if (!surname.isEmpty()) {
            fields.put("surname", surname);
        }
        if (!givenNames.isEmpty()) {
            fields.put("givenNames", givenNames);
        }

        return Optional.of(new MrzResult(fields, l1 + "\n" + l2 + "\n" + l3));
    }

    private static List<String> candidateLines(String rawText) {
        List<String> out = new ArrayList<>();
        for (String raw : rawText.split("\\r?\\n")) {
            String cleaned = raw
                .toUpperCase()
                .replace(' ', '<')
                .replaceAll("[^A-Z0-9<]", "");
            if (cleaned.length() >= LINE_LEN - 2 && cleaned.length() <= LINE_LEN + 2) {
                out.add(cleaned);
            }
        }
        return out;
    }

    private static String pad(String s) {
        if (s.length() == LINE_LEN) {
            return s;
        }
        if (s.length() > LINE_LEN) {
            return s.substring(0, LINE_LEN);
        }
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < LINE_LEN) {
            sb.append('<');
        }
        return sb.toString();
    }

    /**
     * ICAO 9303 check-digit algorithm. Weights 7, 3, 1 cycling. {@code <} = 0,
     * digits map to themselves, letters A..Z map to 10..35.
     */
    public static int computeCheckDigit(String s) {
        int sum = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int v;
            if (c == '<') {
                v = 0;
            } else if (c >= '0' && c <= '9') {
                v = c - '0';
            } else if (c >= 'A' && c <= 'Z') {
                v = 10 + (c - 'A');
            } else {
                return -1;
            }
            sum += v * WEIGHTS[i % WEIGHTS.length];
        }
        return sum % 10;
    }

    private static boolean checkDigitMatches(String data, char expected) {
        if (expected == '<') {
            // The standard allows '<' as filler when no check is computed (rare).
            return true;
        }
        if (expected < '0' || expected > '9') {
            return false;
        }
        int computed = computeCheckDigit(data);
        return computed >= 0 && computed == (expected - '0');
    }

    private static String[] splitNames(String l3) {
        // Surname and given names separated by '<<'; trailing '<' fillers stripped.
        int sep = l3.indexOf("<<");
        String surname;
        String given;
        if (sep < 0) {
            surname = l3.replace('<', ' ').trim();
            given = "";
        } else {
            surname = l3.substring(0, sep).replace('<', ' ').trim();
            given = l3.substring(sep + 2).replace('<', ' ').trim().replaceAll("\\s+", " ");
        }
        return new String[]{surname, given};
    }

    private static String expandDate(String yymmdd, boolean expiry) {
        if (yymmdd.indexOf('<') >= 0 || yymmdd.length() != 6) {
            return null;
        }
        try {
            int yy = Integer.parseInt(yymmdd.substring(0, 2));
            int mm = Integer.parseInt(yymmdd.substring(2, 4));
            int dd = Integer.parseInt(yymmdd.substring(4, 6));
            int year;
            if (expiry) {
                year = 2000 + yy;
            } else {
                int currentYy = Year.now().getValue() % 100;
                year = (yy > currentYy) ? 1900 + yy : 2000 + yy;
            }
            return LocalDate.of(year, mm, dd).toString();
        } catch (NumberFormatException | java.time.DateTimeException ex) {
            return null;
        }
    }

    private static String normaliseSex(char c) {
        return switch (c) {
            case 'M' -> "M";
            case 'F' -> "F";
            case '<' -> "X";
            default -> String.valueOf(c);
        };
    }

    private static String normaliseDocNumber(String raw) {
        return raw.replace("<", "");
    }

    /** Outcome of a successful MRZ parse. */
    public record MrzResult(Map<String, String> fields, String mrzText) {
        public MrzResult {
            fields = Map.copyOf(fields);
        }
    }
}
