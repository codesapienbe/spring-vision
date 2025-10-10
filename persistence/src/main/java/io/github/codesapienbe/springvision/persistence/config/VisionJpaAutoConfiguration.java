package io.github.codesapienbe.springvision.persistence.config;

import io.github.codesapienbe.springvision.persistence.service.JpaVectorSimilarityService;
import io.github.codesapienbe.springvision.persistence.service.MySQLVectorSimilarityService;
import io.github.codesapienbe.springvision.persistence.service.OracleVectorSimilarityService;
import io.github.codesapienbe.springvision.persistence.service.PostgreSQLVectorSimilarityService;
import io.github.codesapienbe.springvision.persistence.service.DatabaseVendorDetector;
import io.github.codesapienbe.springvision.persistence.service.VectorSimilarityService;
import io.github.codesapienbe.springvision.persistence.config.VectorSimilarityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for creating the appropriate {@link VectorSimilarityService} bean.
 * This configuration detects the underlying database vendor and instantiates a specialized
 * vector service if available (e.g., for PostgreSQL with pgvector), otherwise it falls back
 * to a generic JPA-based implementation.
 */
@Configuration
@EnableConfigurationProperties(VectorSimilarityProperties.class)
public class VisionJpaAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VisionJpaAutoConfiguration.class);

    /**
     * Creates and configures the primary {@link VectorSimilarityService} bean.
     * It inspects the detected database vendor and the configured provider to select the most
     * suitable implementation. If a specific provider bean is available (e.g., for PostgreSQL),
     * it will be used. Otherwise, it defaults to the generic {@link JpaVectorSimilarityService}.
     *
     * @param properties The vector similarity configuration properties.
     * @param repository The face embedding repository.
     * @param vendorDetector The database vendor detector.
     * @param pgProvider A provider for the PostgreSQL-specific service.
     * @param oracleProvider A provider for the Oracle-specific service.
     * @param mysqlProvider A provider for the MySQL-specific service.
     * @param jpaProvider A provider for the generic JPA service.
     * @param adapterRegistry A registry for native vector adapters.
     * @return The configured {@link VectorSimilarityService} instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public VectorSimilarityService vectorSimilarityService(
        VectorSimilarityProperties properties,
        io.github.codesapienbe.springvision.persistence.repository.FaceEmbeddingRepository repository,
        DatabaseVendorDetector vendorDetector,
        ObjectProvider<PostgreSQLVectorSimilarityService> pgProvider,
        ObjectProvider<OracleVectorSimilarityService> oracleProvider,
        ObjectProvider<MySQLVectorSimilarityService> mysqlProvider,
        ObjectProvider<JpaVectorSimilarityService> jpaProvider,
        io.github.codesapienbe.springvision.persistence.service.NativeVectorAdapterRegistry adapterRegistry) {

        io.github.codesapienbe.springvision.persistence.enums.DatabaseVendor vendor = vendorDetector.detectVendor();
        String configuredProvider = properties.getProvider().name().toLowerCase();

        log.info("Detected database vendor: {}", vendor);
        log.info("Configured vector provider: {}", configuredProvider);

        switch (vendor) {
            case POSTGRESQL:
                if (configuredProvider.equals("postgres") && pgProvider.getIfAvailable() != null) {
                    return pgProvider.getIfAvailable();
                }
                break;
            case ORACLE:
                if (configuredProvider.equals("oracle") && oracleProvider.getIfAvailable() != null) {
                    return oracleProvider.getIfAvailable();
                }
                break;
            case MYSQL:
                if (configuredProvider.equals("mysql") && mysqlProvider.getIfAvailable() != null) {
                    return mysqlProvider.getIfAvailable();
                }
                break;
            case H2:
                // H2 doesn't provide native vector support; if explicitly configured
                // to 'h2' prefer the H2-variant service if available, otherwise fall
                // back to the JPA implementation.
                if (configuredProvider.equals("h2")) {
                    // Try to obtain a JPA-backed provider (H2VectorSimilarityService is conditional)
                    VectorSimilarityService h2svc = jpaProvider.getIfAvailable();
                    if (h2svc != null) return h2svc;
                }
                break;
            default:
                break;
        }

        // Fallback to JPA implementation
        JpaVectorSimilarityService jpa = jpaProvider.getIfAvailable();
        if (jpa != null) return jpa;

        // As a last resort create a new JpaVectorSimilarityService using repository
        log.warn("Falling back to programmatic JpaVectorSimilarityService instance");
        return new JpaVectorSimilarityService(repository);
    }

}
