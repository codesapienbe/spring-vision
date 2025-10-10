package io.github.codesapienbe.springvision.persistence.service;

/**
 * Defines a contract for adapting a portable byte array representation of a vector into a database-specific format.
 * Implementations of this interface are responsible for handling the nuances of how different databases (e.g., PostgreSQL, Oracle)
 * store and query vector data, allowing the core services to remain database-agnostic.
 */
public interface NativeVectorAdapter {

    /**
     * Returns the unique identifier for the database provider this adapter supports (e.g., "postgres", "oracle", "mysql").
     *
     * @return The provider ID string.
     */
    String provider();

    /**
     * Converts a native vector from its portable byte array format into a format suitable for use as a query parameter
     * for the specific database provider. The return type may be a String, byte array, or a provider-specific object.
     *
     * @param nativeVector The portable byte array representation of the vector.
     * @return An object representing the vector in a query-compatible format.
     */
    Object toQueryParam(byte[] nativeVector);

    /**
     * Converts a native vector from its portable byte array format into a format suitable for insertion into a native
     * vector column for the specific database provider. The return type can vary (e.g., String, byte[], or a provider-specific object like PGobject).
     *
     * @param nativeVector The portable byte array representation of the vector.
     * @return An object representing the vector in a format suitable for database insertion.
     */
    Object toInsertValue(byte[] nativeVector);
}
