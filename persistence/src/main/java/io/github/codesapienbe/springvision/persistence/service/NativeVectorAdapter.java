package io.github.codesapienbe.springvision.persistence.service;

/**
 * Adapter interface to convert the portable native vector (byte[]) into
 * provider-specific representations for queries and inserts.
 */
public interface NativeVectorAdapter {

    /**
     * Gets the provider identifier (e.g. "postgres", "oracle", "mysql").
     *
     * @return the provider ID
     */
    String provider();

    /**
     * Converts native vector bytes to a query parameter acceptable for the provider.
     * Return type may be String or byte[] depending on provider.
     *
     * @param nativeVector the native vector bytes
     * @return the query parameter object
     */
    Object toQueryParam(byte[] nativeVector);

    /**
     * Converts native vector bytes to a value suitable for inserting into the native column.
     * Return type may vary (String, byte[] or provider-specific object).
     *
     * @param nativeVector the native vector bytes
     * @return the insert value object
     */
    Object toInsertValue(byte[] nativeVector);
}
