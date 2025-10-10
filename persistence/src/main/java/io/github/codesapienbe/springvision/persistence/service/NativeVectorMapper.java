package io.github.codesapienbe.springvision.persistence.service;

import io.github.codesapienbe.springvision.persistence.util.VectorUtils;

/**
 * A utility class for mapping a portable byte array representation of a vector into provider-specific formats
 * (e.g., PostgreSQL vector string, MySQL JSON array, Oracle byte array) and vice-versa.
 */
public final class NativeVectorMapper {

    private NativeVectorMapper() {
    }

    /**
     * Deserializes a byte array into a float array.
     *
     * @param bytes The byte array to deserialize.
     * @return The resulting float array.
     */
    public static float[] bytesToFloatArray(byte[] bytes) {
        return VectorUtils.deserializeFloatArray(bytes);
    }

    /**
     * Converts a float array embedding into a string format suitable for PostgreSQL's pgvector extension (e.g., "[1.0,2.0,3.0]").
     *
     * @param embedding The float array to convert.
     * @return The PostgreSQL vector string representation.
     */
    public static String toPostgresVectorString(float[] embedding) {
        if (embedding == null) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(Float.toString(embedding[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Converts a native byte array representation of a vector into a PostgreSQL vector string.
     *
     * @param nativeBytes The native byte array to convert.
     * @return The PostgreSQL vector string representation.
     */
    public static String toPostgresVectorString(byte[] nativeBytes) {
        return toPostgresVectorString(bytesToFloatArray(nativeBytes));
    }

    // Backwards-compatible aliases

    /**
     * Alias for {@link #toPostgresVectorString(float[])}.
     *
     * @param embedding The float array to convert.
     * @return The PostgreSQL vector string representation.
     */
    public static String toPgVectorString(float[] embedding) {
        return toPostgresVectorString(embedding);
    }

    /**
     * Alias for {@link #toPostgresVectorString(byte[])}.
     *
     * @param nativeBytes The native byte array to convert.
     * @return The PostgreSQL vector string representation.
     */
    public static String toPgVectorString(byte[] nativeBytes) {
        return toPostgresVectorString(nativeBytes);
    }

    /**
     * Converts a float array embedding into a JSON array string format for MySQL.
     *
     * @param embedding The float array to convert.
     * @return The JSON string representation.
     */
    public static String toMySqlJson(float[] embedding) {
        if (embedding == null) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(Float.toString(embedding[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Converts a native byte array representation of a vector into a MySQL JSON array string.
     *
     * @param nativeBytes The native vector bytes.
     * @return The JSON string representation.
     */
    public static String toMySqlJson(byte[] nativeBytes) {
        return toMySqlJson(bytesToFloatArray(nativeBytes));
    }

    /**
     * Converts a float array embedding into a raw byte array for Oracle Database.
     *
     * @param embedding The float array to convert.
     * @return The raw byte array representation.
     */
    public static byte[] toOracleBytes(float[] embedding) {
        return VectorConversionHelpers.serializeFloatArrayToBytes(embedding);
    }

    /**
     * Converts a native byte array representation into a format suitable for Oracle.
     * Currently, this is an identity operation as the native format is already a serialized float array.
     *
     * @param nativeBytes The native byte array.
     * @return The resulting byte array for Oracle.
     */
    public static byte[] toOracleBytes(byte[] nativeBytes) {
        // currently nativeBytes already in serialized float[] form
        return nativeBytes == null ? new byte[0] : nativeBytes;
    }
}
