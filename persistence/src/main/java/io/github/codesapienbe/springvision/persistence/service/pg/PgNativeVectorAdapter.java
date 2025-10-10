package io.github.codesapienbe.springvision.persistence.service.pg;

import io.github.codesapienbe.springvision.persistence.service.NativeVectorAdapter;
import io.github.codesapienbe.springvision.persistence.service.NativeVectorMapper;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Adapter for the PostgreSQL vector provider ("postgres").
 * This adapter converts the generic byte array representation of a vector into a format
 * that is usable by the pgvector extension in PostgreSQL.
 */
@Component
public class PgNativeVectorAdapter implements NativeVectorAdapter {

    /**
     * {@inheritDoc}
     */
    @Override
    public String provider() {
        return "postgres";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Converts the native vector byte array into a string representation suitable for a query parameter (e.g., "[1.0,2.0,3.0]").
     */
    @Override
    public Object toQueryParam(byte[] nativeVector) {
        return NativeVectorMapper.toPostgresVectorString(nativeVector);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Converts the native vector byte array into a {@code org.postgresql.util.PGobject} of type "vector".
     * This is the recommended way to pass vector data to the PostgreSQL JDBC driver for use with pgvector.
     * If the PostgreSQL driver is not available at runtime, it falls back to a string representation.
     */
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
