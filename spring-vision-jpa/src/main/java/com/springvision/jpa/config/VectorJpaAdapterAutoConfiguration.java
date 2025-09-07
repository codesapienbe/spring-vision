package com.springvision.jpa.config;

import com.springvision.core.VectorService;
import com.springvision.core.VisionTemplate;
import com.springvision.jpa.adapters.VectorServiceAdapter;
import com.springvision.jpa.service.PostgreSQLVectorSimilarityService;
import com.springvision.jpa.service.JpaVectorSimilarityService;
import com.springvision.jpa.service.MySQLVectorSimilarityService;
import com.springvision.jpa.service.OracleVectorSimilarityService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes core VectorService adapter beans when module VectorSimilarityService implementations are present.
 */
@Configuration
public class VectorJpaAdapterAutoConfiguration {

    @Bean
    @ConditionalOnBean(JpaVectorSimilarityService.class)
    @ConditionalOnMissingBean(VectorService.class)
    public VectorService jpaVectorServiceAdapter(JpaVectorSimilarityService svc) {
        return new VectorServiceAdapter(svc);
    }

    @Bean
    @ConditionalOnBean(PostgreSQLVectorSimilarityService.class)
    @ConditionalOnMissingBean(VectorService.class)
    public VectorService pgVectorServiceAdapter(PostgreSQLVectorSimilarityService svc) {
        return new VectorServiceAdapter(svc);
    }

    @Bean
    @ConditionalOnBean(OracleVectorSimilarityService.class)
    @ConditionalOnMissingBean(VectorService.class)
    public VectorService oracleVectorServiceAdapter(OracleVectorSimilarityService svc) {
        return new VectorServiceAdapter(svc);
    }

    @Bean
    @ConditionalOnBean(MySQLVectorSimilarityService.class)
    @ConditionalOnMissingBean(VectorService.class)
    public VectorService mysqlVectorServiceAdapter(MySQLVectorSimilarityService svc) {
        return new VectorServiceAdapter(svc);
    }

    @Bean
    @ConditionalOnMissingBean(VisionTemplate.class)
    public VisionTemplate visionTemplate(org.springframework.beans.factory.ObjectProvider<com.springvision.core.VisionBackend> backendProvider,
                                         ObjectProvider<VectorService> vectorServiceProvider) {
        com.springvision.core.VisionBackend backend = backendProvider.getIfAvailable();
        if (backend == null) {
            throw new IllegalStateException("No VisionBackend available to create VisionTemplate");
        }
        VectorService vs = vectorServiceProvider.getIfAvailable();
        if (vs != null) {
            return new VisionTemplate(backend, vs);
        }
        return new VisionTemplate(backend);
    }
} 