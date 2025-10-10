package io.github.codesapienbe.springvision.persistence.service;

import io.github.codesapienbe.springvision.persistence.enums.DatabaseVendor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Detect the database vendor by connecting and inspecting JDBC URL.
 */
@Component
public class DatabaseVendorDetector {

    private final DataSource dataSource;

    /**
     * Constructs a database vendor detector.
     *
     * @param dataSource the data source to inspect
     */
    public DatabaseVendorDetector(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Detects the database vendor from the JDBC connection URL.
     *
     * @return the detected database vendor
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
