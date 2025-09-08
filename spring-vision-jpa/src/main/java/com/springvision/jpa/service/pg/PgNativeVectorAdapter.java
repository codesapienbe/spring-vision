package com.springvision.jpa.service.pg;

import com.springvision.jpa.service.NativeVectorAdapter;
import com.springvision.jpa.service.NativeVectorMapper;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Adapter for PostgreSQL pgvector provider.
 */
@Component
public class PgNativeVectorAdapter implements NativeVectorAdapter {

    @Override
    public String provider() {
        return "pgvector";
    }

    @Override
    public Object toQueryParam(byte[] nativeVector) {
        return NativeVectorMapper.toPgVectorString(nativeVector);
    }

    @Override
    public Object toInsertValue(byte[] nativeVector) {
        String vec = NativeVectorMapper.toPgVectorString(nativeVector);
        try {
            Class<?> pgObjectClass = Class.forName("org.postgresql.util.PGobject");
            Object pgObject = pgObjectClass.getDeclaredConstructor().newInstance();
            Method setType = pgObjectClass.getMethod("setType", String.class);
            Method setValue = pgObjectClass.getMethod("setValue", String.class);
            setType.invoke(pgObject, "vector");
            setValue.invoke(pgObject, vec);
            return pgObject;
        } catch (Exception e) {
            // Postgres driver unavailable at compile time — fallback to string
            return vec;
        }
    }
} 