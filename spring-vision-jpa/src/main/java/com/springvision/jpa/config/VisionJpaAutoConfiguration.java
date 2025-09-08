package com.springvision.jpa.config;

import com.springvision.jpa.service.JpaVectorSimilarityService;
import com.springvision.jpa.service.MySQLVectorSimilarityService;
import com.springvision.jpa.service.OracleVectorSimilarityService;
import com.springvision.jpa.service.PostgreSQLVectorSimilarityService;
import com.springvision.jpa.service.DatabaseVendorDetector;
import com.springvision.jpa.service.VectorSimilarityService;
import com.springvision.jpa.config.VectorSimilarityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for JPA vector similarity services.
 */
@Configuration
@EnableConfigurationProperties(VectorSimilarityProperties.class)
public class VisionJpaAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VisionJpaAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public VectorSimilarityService vectorSimilarityService(
            VectorSimilarityProperties properties,
            com.springvision.jpa.repository.FaceEmbeddingRepository repository,
            DatabaseVendorDetector vendorDetector,
            ObjectProvider<PostgreSQLVectorSimilarityService> pgProvider,
            ObjectProvider<OracleVectorSimilarityService> oracleProvider,
            ObjectProvider<MySQLVectorSimilarityService> mysqlProvider,
            ObjectProvider<JpaVectorSimilarityService> jpaProvider,
            com.springvision.jpa.service.NativeVectorAdapterRegistry adapterRegistry) {

        com.springvision.jpa.enums.DatabaseVendor vendor = vendorDetector.detectVendor();
        String configuredProvider = properties.getProvider().name().toLowerCase();

        log.info("Detected database vendor: {}", vendor);
        log.info("Configured vector provider: {}", configuredProvider);

        switch (vendor) {
            case POSTGRESQL:
                if (configuredProvider.equals("pgvector") && pgProvider.getIfAvailable() != null) {
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