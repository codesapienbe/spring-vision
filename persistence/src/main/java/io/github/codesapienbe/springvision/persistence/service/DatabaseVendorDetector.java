package io.github.codesapienbe.springvision.persistence.service;

import io.github.codesapienbe.springvision.persistence.enums.DatabaseVendor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Detects the underlying database vendor by inspecting the JDBC connection URL.
 * This component is used to dynamically select the appropriate vector similarity service and schema management logic.
 */
@Component
public class DatabaseVendorDetector {

    private final DataSource dataSource;

    /**
     * Constructs a new DatabaseVendorDetector.
     *
     * @param dataSource The {@link DataSource} to be inspected.
     */
    public DatabaseVendorDetector(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Detects the database vendor from the JDBC connection URL.
     * It establishes a temporary connection to retrieve the database metadata and URL.
     *
     * @return The detected {@link DatabaseVendor}, or {@link DatabaseVendor#UNKNOWN} if detection fails or the vendor is not supported.
     */
    public DatabaseVendor detectVendor() {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL().toLowerCase();
            if (url.contains("postgresql")) return DatabaseVendor.POSTGRESQL;
            if (url.contains("oracle")) return DatabaseVendor.ORACLE;
            if (url.contains("mysql")) return DatabaseVendor.MYSQL;
            if (url.contains("h2")) return DatabaseVendor.H2;
            if (url.contains("hsqldb")) return DatabaseVendor.HSQLDB;
            return DatabaseVendor.UNKNOWN;
        } catch (Exception e) {
            return DatabaseVendor.UNKNOWN;
        }
    }
}
