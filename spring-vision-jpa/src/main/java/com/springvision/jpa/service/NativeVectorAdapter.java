package com.springvision.jpa.service;

/**
 * Adapter interface to convert the portable native vector (byte[]) into
 * provider-specific representations for queries and inserts.
 */
public interface NativeVectorAdapter {

    /**
     * Provider id (e.g. "pgvector", "oracle", "mysql").
     */
    String provider();

    /**
     * Convert native vector bytes to a query parameter acceptable for the provider.
     * Return type may be String or byte[] depending on provider.
     */
    Object toQueryParam(byte[] nativeVector);

    /**
     * Convert native vector bytes to a value suitable for inserting into the native column.
     * Return type may vary (String, byte[] or provider-specific object).
     */
    Object toInsertValue(byte[] nativeVector);
} 