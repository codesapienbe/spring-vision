package io.github.codesapienbe.springvision.persistence.enums;

/**
 * Database vendor detection enum used by JPA module.
 */
public enum DatabaseVendor {
    /**
     * PostgreSQL database.
     */
    POSTGRESQL,
    /**
     * Oracle database.
     */
    ORACLE,
    /**
     * MySQL database.
     */
    MYSQL,
    /**
     * H2 database.
     */
    H2,
    /**
     * HSQLDB database.
     */
    HSQLDB,
    /**
     * Unknown or unsupported database.
     */
    UNKNOWN
}
