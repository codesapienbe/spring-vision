package com.springvision.jpa.config;

import com.springvision.jpa.service.*;
import com.springvision.jpa.service.VectorSimilarityService.VectorServiceHealth;
import com.springvision.jpa.service.VectorSimilarityService.VerificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Auto-configuration for JPA vector similarity services.
 */
@Configuration
@EnableConfigurationProperties(VectorSimilarityProperties.class)
public class VisionJpaAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VisionJpaAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public com.springvision.jpa.service.VectorSimilarityService vectorSimilarityService(
            VectorSimilarityProperties properties,
            com.springvision.jpa.repository.FaceEmbeddingRepository repository,
            com.springvision.jpa.service.DatabaseVendorDetector vendorDetector,
            ObjectProvider<PostgreSQLVectorSimilarityService> pgProvider,
            ObjectProvider<OracleVectorSimilarityService> oracleProvider,
            ObjectProvider<MySQLVectorSimilarityService> mysqlProvider,
            ObjectProvider<JpaVectorSimilarityService> jpaProvider) {

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
            default:
                break;
        }

        // Fallback to JPA implementation
        com.springvision.jpa.service.VectorSimilarityService jpa = jpaProvider.getIfAvailable();
        if (jpa != null) return jpa;

        // As a last resort create a new JpaVectorSimilarityService using repository
        log.warn("Falling back to programmatic JpaVectorSimilarityService instance");
        return new JpaVectorSimilarityService(repository);
    }
} 