package com.springvision.jpa.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Small helper utilities for converting vectors to database-specific formats.
 */
public final class VectorConversionHelpers {

    private VectorConversionHelpers() {}

    public static byte[] serializeFloatArrayToBytes(float[] array) {
        if (array == null) return new byte[0];
        ByteBuffer buffer = ByteBuffer.allocate(array.length * Float.BYTES).order(ByteOrder.BIG_ENDIAN);
        for (float v : array) buffer.putFloat(v);
        return buffer.array();
    }
} 