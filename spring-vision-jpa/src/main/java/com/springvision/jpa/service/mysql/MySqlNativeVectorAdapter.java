package com.springvision.jpa.service.mysql;

import com.springvision.jpa.service.NativeVectorAdapter;
import com.springvision.jpa.service.NativeVectorMapper;
import org.springframework.stereotype.Component;

/**
 * Adapter for MySQL provider using JSON array representation.
 */
@Component
public class MySqlNativeVectorAdapter implements NativeVectorAdapter {

    @Override
    public String provider() {
        return "mysql";
    }

    @Override
    public Object toQueryParam(byte[] nativeVector) {
        return NativeVectorMapper.toMySqlJson(nativeVector);
    }

    @Override
    public Object toInsertValue(byte[] nativeVector) {
        return NativeVectorMapper.toMySqlJson(nativeVector);
    }
} 