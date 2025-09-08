package com.springvision.jpa.service.oracle;

import com.springvision.jpa.service.NativeVectorAdapter;
import com.springvision.jpa.service.NativeVectorMapper;
import com.springvision.jpa.service.VectorConversionHelpers;
import org.springframework.stereotype.Component;

/**
 * Adapter for Oracle provider (placeholder) — returns byte[] for oracle vector column.
 */
@Component
public class OracleNativeVectorAdapter implements NativeVectorAdapter {

    @Override
    public String provider() {
        return "oracle";
    }

    @Override
    public Object toQueryParam(byte[] nativeVector) {
        return VectorConversionHelpers.serializeFloatArrayToBytes(NativeVectorMapper.bytesToFloatArray(nativeVector));
    }

    @Override
    public Object toInsertValue(byte[] nativeVector) {
        return VectorConversionHelpers.serializeFloatArrayToBytes(NativeVectorMapper.bytesToFloatArray(nativeVector));
    }
} 