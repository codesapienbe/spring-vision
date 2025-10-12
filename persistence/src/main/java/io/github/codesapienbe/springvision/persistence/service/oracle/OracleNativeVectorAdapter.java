package io.github.codesapienbe.springvision.persistence.service.oracle;

import io.github.codesapienbe.springvision.persistence.service.NativeVectorAdapter;
import io.github.codesapienbe.springvision.persistence.service.NativeVectorMapper;
import io.github.codesapienbe.springvision.persistence.service.VectorConversionHelpers;
import org.springframework.stereotype.Component;

/**
 * Adapter for the Oracle vector provider ("oracle").
 * This adapter handles the conversion of a generic vector byte array into a format suitable for Oracle Database's native vector type.
 * It currently returns the vector as a raw byte array, which is a common requirement for Oracle's vector type.
 */
@Component
public class OracleNativeVectorAdapter implements NativeVectorAdapter {

    /**
     * Default constructor for OracleNativeVectorAdapter.
     */
    public OracleNativeVectorAdapter() {
        // Default constructor
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String provider() {
        return "oracle";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Converts the generic vector representation into a raw byte array suitable for Oracle vector query parameters.
     */
    @Override
    public Object toQueryParam(byte[] nativeVector) {
        return VectorConversionHelpers.serializeFloatArrayToBytes(NativeVectorMapper.bytesToFloatArray(nativeVector));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Converts the generic vector representation into a raw byte array for insertion into an Oracle vector column.
     */
    @Override
    public Object toInsertValue(byte[] nativeVector) {
        return VectorConversionHelpers.serializeFloatArrayToBytes(NativeVectorMapper.bytesToFloatArray(nativeVector));
    }
}
