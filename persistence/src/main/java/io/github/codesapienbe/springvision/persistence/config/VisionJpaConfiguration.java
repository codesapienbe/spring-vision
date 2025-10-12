package io.github.codesapienbe.springvision.persistence.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configures the core JPA settings for the Spring Vision persistence module.
 * This class enables JPA repositories and auditing, and is activated when `spring.vision.jpa.enabled` is true.
 */
@Configuration
@EnableJpaRepositories(basePackages = "io.github.codesapienbe.springvision.persistence.repository")
@EnableJpaAuditing
@ConditionalOnProperty(value = "spring.vision.jpa.enabled", havingValue = "true", matchIfMissing = true)
public class VisionJpaConfiguration {

    /**
     * Default constructor for VisionJpaConfiguration.
     */
    public VisionJpaConfiguration() {
        // Basic configuration only; beans will be provided by Spring Boot and module auto-configuration.
    }
}
