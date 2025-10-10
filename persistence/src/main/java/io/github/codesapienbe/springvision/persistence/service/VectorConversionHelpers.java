package io.github.codesapienbe.springvision.persistence.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Provides helper methods for converting vector data types into formats suitable for database storage.
 */
public final class VectorConversionHelpers {

    private VectorConversionHelpers() {
    }

    /**
     * Serializes a float array into a byte array using big-endian byte order.
     * This creates a portable binary representation of the vector.
     *
     * @param array The float array to serialize.
     * @return A byte array representing the float array.
     */
    public static byte[] serializeFloatArrayToBytes(float[] array) {
        if (array == null) return new byte[0];
        ByteBuffer buffer = ByteBuffer.allocate(array.length * Float.BYTES).order(ByteOrder.BIG_ENDIAN);
        for (float v : array) buffer.putFloat(v);
        return buffer.array();
    }
}
