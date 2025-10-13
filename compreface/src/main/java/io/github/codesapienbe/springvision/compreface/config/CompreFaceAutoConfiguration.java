package io.github.codesapienbe.springvision.compreface.config;

import io.github.codesapienbe.springvision.compreface.CompreFaceBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/**
 * Auto-configuration for Spring Vision CompreFace module.
 *
 * <p>This configuration is activated when:
 * <ul>
 *   <li>spring.vision.compreface.enabled=true</li>
 *   <li>CompreFaceBackend class is on the classpath</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(CompreFaceBackend.class)
@ConditionalOnProperty(prefix = "spring.vision.compreface", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CompreFaceProperties.class)
public class CompreFaceAutoConfiguration {

    /**
     * Default constructor for {@link CompreFaceAutoConfiguration}.
     */
    public CompreFaceAutoConfiguration() {
        // Default constructor
    }

    /**
     * Creates CompreFaceBackend bean if not already defined.
     *
     * @param properties the CompreFace configuration properties
     * @return configured CompreFaceBackend instance
     */
    @Bean
    @ConditionalOnMissingBean
    public CompreFaceBackend compreFaceBackend(CompreFaceProperties properties) {
        return new CompreFaceBackend(properties.baseUrl(), Duration.ofSeconds(properties.timeout()));
    }

    /**
     * Creates VisionTemplate bean configured with CompreFaceBackend if not already defined.
     *
     * @param backend the CompreFace backend
     * @return configured VisionTemplate instance
     */
    @Bean
    @ConditionalOnMissingBean(name = "compreFaceVisionTemplate")
    public VisionTemplate compreFaceVisionTemplate(CompreFaceBackend backend) {
        return new VisionTemplate(backend);
    }
}
