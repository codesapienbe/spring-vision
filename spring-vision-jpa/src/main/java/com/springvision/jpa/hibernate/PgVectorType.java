package com.springvision.jpa.hibernate;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Placeholder PGVector type helper. Full Hibernate UserType implementation
 * will be added in a later batch. This stub provides small helpers to parse
 * vector strings returned by native queries.
 */
public final class PgVectorType {

    private PgVectorType() { }

    public static float[] parsePgVector(String vectorString) throws SQLException {
        if (vectorString == null) return null;
        // Expect format like: [0.1,0.2,0.3]
        String trimmed = vectorString.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String inner = trimmed.substring(1, trimmed.length() - 1).trim();
            if (inner.isEmpty()) return new float[0];
            String[] parts = inner.split(",");
            float[] arr = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                arr[i] = Float.parseFloat(parts[i]);
            }
            return arr;
        }
        throw new SQLException("Invalid pgvector format: " + vectorString);
    }
} 