package io.github.codesapienbe.springvision.persistence.config;

import io.github.codesapienbe.springvision.core.VectorService;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.persistence.adapters.VectorServiceAdapter;
import io.github.codesapienbe.springvision.persistence.service.PostgreSQLVectorSimilarityService;
import io.github.codesapienbe.springvision.persistence.service.JpaVectorSimilarityService;
import io.github.codesapienbe.springvision.persistence.service.MySQLVectorSimilarityService;
import io.github.codesapienbe.springvision.persistence.service.OracleVectorSimilarityService;
import io.github.codesapienbe.springvision.persistence.service.H2VectorSimilarityService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Exposes core VectorService adapter beans when module VectorSimilarityService implementations are present.
 */
@Configuration
public class VectorJpaAdapterAutoConfiguration {

    @Bean
    @ConditionalOnBean(JpaVectorSimilarityService.class)
    @ConditionalOnMissingBean(VectorService.class)
    public VectorService jpaVectorServiceAdapter(java.util.List<JpaVectorSimilarityService> services,
                                                 VectorSimilarityProperties properties) {
        if (services == null || services.isEmpty()) return null;
        // If tests/configure H2 explicitly, prefer the H2-specific service implementation
        try {
            if (properties.getProvider() == VectorSimilarityProperties.VectorProvider.H2) {
                for (JpaVectorSimilarityService s : services) {
                    if (s.getClass().getSimpleName().equals("H2VectorSimilarityService")) {
                        return new VectorServiceAdapter(s);
                    }
                }
            }
        } catch (Exception ignored) {
            // ignore and fall back to default selection
        }

        // Prefer a non-H2 Jpa implementation if available, otherwise pick the first
        for (JpaVectorSimilarityService s : services) {
            if (!s.getClass().getSimpleName().equals("H2VectorSimilarityService")) {
                return new VectorServiceAdapter(s);
            }
        }
        return new VectorServiceAdapter(services.get(0));
    }

    @Bean
    @ConditionalOnBean(PostgreSQLVectorSimilarityService.class)
    @ConditionalOnMissingBean(VectorService.class)
    public VectorService postgresServiceAdapter(PostgreSQLVectorSimilarityService svc) {
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

    /**
     * Adapter bean for H2-backed vector similarity service used in tests/dev.
     */
    @Bean
    @ConditionalOnBean(H2VectorSimilarityService.class)
    @ConditionalOnMissingBean(VectorService.class)
    public VectorService h2VectorServiceAdapter(H2VectorSimilarityService svc) {
        return new VectorServiceAdapter(svc);
    }

    @Bean("originalVisionTemplate")
    @ConditionalOnMissingBean(name = "originalVisionTemplate")
    public VisionTemplate originalVisionTemplate(ObjectProvider<VisionBackend> backendProvider) {
        VisionBackend backend = backendProvider.getIfAvailable();
        if (backend == null) {
            throw new IllegalStateException("No VisionBackend available to create originalVisionTemplate");
        }
        return new VisionTemplate(backend);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(value = "spring.vision.jpa.enhanced-template", havingValue = "true", matchIfMissing = true)
    public VisionTemplate createEnhancedVisionTemplate(@Qualifier("originalVisionTemplate") VisionTemplate originalTemplate,
                                                       ObjectProvider<VectorService> vectorServiceProvider) {
        VectorService vs = vectorServiceProvider.getIfAvailable();
        if (vs != null) {
            return new VisionTemplate(originalTemplate.getBackend(), vs);
        }
        return originalTemplate;
    }

}
