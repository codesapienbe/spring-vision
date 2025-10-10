package io.github.codesapienbe.springvision.persistence.service.oracle;

import io.github.codesapienbe.springvision.persistence.service.NativeVectorAdapter;
import io.github.codesapienbe.springvision.persistence.service.NativeVectorMapper;
import io.github.codesapienbe.springvision.persistence.service.VectorConversionHelpers;
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
