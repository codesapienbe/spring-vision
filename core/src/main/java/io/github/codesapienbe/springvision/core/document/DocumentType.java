package io.github.codesapienbe.springvision.core.document;

/**
 * Supported identity document layouts.
 *
 * <p>Used as the type tag on {@link ParsedDocument} and surfaced in the
 * MCP / REST response under the {@code documentType} key.</p>
 */
public enum DocumentType {

    BELGIAN_EID(Category.IDENTITY_CARD, "BE"),
    BELGIAN_DRIVER_LICENSE(Category.DRIVER_LICENSE, "BE"),
    DUTCH_EID(Category.IDENTITY_CARD, "NL"),
    DUTCH_DRIVER_LICENSE(Category.DRIVER_LICENSE, "NL"),
    LUX_EID(Category.IDENTITY_CARD, "LU"),
    LUX_DRIVER_LICENSE(Category.DRIVER_LICENSE, "LU"),
    UNKNOWN(Category.UNKNOWN, null);

    public enum Category { IDENTITY_CARD, DRIVER_LICENSE, UNKNOWN }

    private final Category category;
    private final String countryCode;

    DocumentType(Category category, String countryCode) {
        this.category = category;
        this.countryCode = countryCode;
    }

    public Category category() {
        return category;
    }

    /**
     * @return ISO 3166-1 alpha-2 country code or {@code null} for {@link #UNKNOWN}
     */
    public String countryCode() {
        return countryCode;
    }
}
