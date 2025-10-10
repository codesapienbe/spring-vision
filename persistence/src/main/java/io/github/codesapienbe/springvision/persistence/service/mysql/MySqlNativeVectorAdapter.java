package io.github.codesapienbe.springvision.persistence.service.mysql;

import io.github.codesapienbe.springvision.persistence.service.NativeVectorAdapter;
import io.github.codesapienbe.springvision.persistence.service.NativeVectorMapper;
import org.springframework.stereotype.Component;

/**
 * Adapter for the MySQL vector provider ("mysql").
 * This adapter converts the generic byte array representation of a vector into a JSON array string,
 * which is a common way to store and query vector data in MySQL.
 */
@Component
public class MySqlNativeVectorAdapter implements NativeVectorAdapter {

    /**
     * Default constructor for the MySQL native vector adapter.
     */
    public MySqlNativeVectorAdapter() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String provider() {
        return "mysql";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Converts the native vector byte array into a JSON array string for use in a query.
     */
    @Override
    public Object toQueryParam(byte[] nativeVector) {
        return NativeVectorMapper.toMySqlJson(nativeVector);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Converts the native vector byte array into a JSON array string for insertion into the database.
     */
    @Override
    public Object toInsertValue(byte[] nativeVector) {
        return NativeVectorMapper.toMySqlJson(nativeVector);
    }
}
