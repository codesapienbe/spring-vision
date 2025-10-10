package io.github.codesapienbe.springvision.persistence.service.pg;

import io.github.codesapienbe.springvision.persistence.service.NativeVectorAdapter;
import io.github.codesapienbe.springvision.persistence.service.NativeVectorMapper;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Adapter for PostgreSQL provider (postgres). Produces a representation usable by
 * pgvector extension when available.
 */
@Component
public class PgNativeVectorAdapter implements NativeVectorAdapter {

    @Override
    public String provider() {
        return "postgres";
    }

    @Override
    public Object toQueryParam(byte[] nativeVector) {
        return NativeVectorMapper.toPostgresVectorString(nativeVector);
    }

    @Override
    public Object toInsertValue(byte[] nativeVector) {
        String vec = NativeVectorMapper.toPostgresVectorString(nativeVector);
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
