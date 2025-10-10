package io.github.codesapienbe.springvision.persistence.service;

import io.github.codesapienbe.springvision.persistence.util.VectorUtils;

/**
 * Utility to map a portable native byte[] vector into provider-specific formats
 * (Postgres vector string, MySQL JSON array, Oracle byte array) and back.
 */
public final class NativeVectorMapper {

    private NativeVectorMapper() {
    }

    /**
     * Converts bytes to a float array.
     *
     * @param bytes the byte array
     * @return the float array
     */
    public static float[] bytesToFloatArray(byte[] bytes) {
        return VectorUtils.deserializeFloatArray(bytes);
    }

    /**
     * Converts the given float array embedding to a Postgres vector string.
     *
     * @param embedding the float array to convert
     * @return the Postgres vector string representation of the float array
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
     * Converts the given native byte array to a Postgres vector string.
     *
     * @param nativeBytes the native byte array to convert
     * @return the Postgres vector string representation of the native byte array
     */
    public static String toPostgresVectorString(byte[] nativeBytes) {
        return toPostgresVectorString(bytesToFloatArray(nativeBytes));
    }

    // Backwards-compatible aliases

    /**
     * Converts the given float array embedding to a Postgres vector string (alias method).
     *
     * @param embedding the float array to convert
     * @return the Postgres vector string representation of the float array
     */
    public static String toPgVectorString(float[] embedding) {
        return toPostgresVectorString(embedding);
    }

    /**
     * Converts the given native byte array to a Postgres vector string (alias method).
     *
     * @param nativeBytes the native byte array to convert
     * @return the Postgres vector string representation of the native byte array
     */
    public static String toPgVectorString(byte[] nativeBytes) {
        return toPostgresVectorString(nativeBytes);
    }

    /**
     * Converts the given float array embedding to a MySQL JSON array format.
     *
     * @param embedding the float array to convert
     * @return the JSON string representation of the float array
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
     * Converts the given native byte array to a MySQL JSON array format.
     *
     * @param nativeBytes the native vector bytes
     * @return the JSON string
     */
    public static String toMySqlJson(byte[] nativeBytes) {
        return toMySqlJson(bytesToFloatArray(nativeBytes));
    }

    /**
     * Converts the given float array embedding to an Oracle byte array.
     *
     * @param embedding the float array to convert
     * @return the byte array representation of the float array
     */
    public static byte[] toOracleBytes(float[] embedding) {
        return VectorConversionHelpers.serializeFloatArrayToBytes(embedding);
    }

    /**
     * Converts the given native byte array to an Oracle byte array.
     *
     * @param nativeBytes the native byte array to convert
     * @return the byte array representation of the native byte array
     */
    public static byte[] toOracleBytes(byte[] nativeBytes) {
        // currently nativeBytes already in serialized float[] form
        return nativeBytes == null ? new byte[0] : nativeBytes;
    }
}
