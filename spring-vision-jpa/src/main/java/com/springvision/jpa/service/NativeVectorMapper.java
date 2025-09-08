package com.springvision.jpa.service;

import com.springvision.jpa.util.VectorUtils;

/**
 * Utility to map a portable native byte[] vector into provider-specific formats
 * (Postgres vector string, MySQL JSON array, Oracle byte array) and back.
 */
public final class NativeVectorMapper {

    private NativeVectorMapper() {}

    public static float[] bytesToFloatArray(byte[] bytes) {
        return VectorUtils.deserializeFloatArray(bytes);
    }

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

    public static String toPostgresVectorString(byte[] nativeBytes) {
        return toPostgresVectorString(bytesToFloatArray(nativeBytes));
    }

    // Backwards-compatible aliases
    public static String toPgVectorString(float[] embedding) { return toPostgresVectorString(embedding); }
    public static String toPgVectorString(byte[] nativeBytes) { return toPostgresVectorString(nativeBytes); }

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

    public static String toMySqlJson(byte[] nativeBytes) {
        return toMySqlJson(bytesToFloatArray(nativeBytes));
    }

    public static byte[] toOracleBytes(float[] embedding) {
        return VectorConversionHelpers.serializeFloatArrayToBytes(embedding);
    }

    public static byte[] toOracleBytes(byte[] nativeBytes) {
        // currently nativeBytes already in serialized float[] form
        return nativeBytes == null ? new byte[0] : nativeBytes;
    }
} 