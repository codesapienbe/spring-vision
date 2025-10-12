package io.github.codesapienbe.springvision.facebytes.config;

import io.github.codesapienbe.springvision.facebytes.FaceBytesBackend;
import io.github.codesapienbe.springvision.facebytes.config.FaceBytesProperties;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Spring Vision FaceBytes module.
 *
 * <p>This configuration is activated when:
 * <ul>
 *   <li>spring.vision.facebytes.enabled=true</li>
 *   <li>FaceBytesBackend class is on the classpath</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(FaceBytesBackend.class)
@ConditionalOnProperty(prefix = "spring.vision.facebytes", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FaceBytesProperties.class)
public class FaceBytesAutoConfiguration {

    /**
     * Default constructor for FaceBytesAutoConfiguration.
     */
    public FaceBytesAutoConfiguration() {
        // Default constructor
    }

    /**
     * Creates FaceBytesBackend bean if not already defined.
     *
     * @param properties the FaceBytes configuration properties
     * @return configured FaceBytesBackend instance
     */
    @Bean
    @ConditionalOnMissingBean
    public FaceBytesBackend faceBytesBackend(FaceBytesProperties properties) {
        return new FaceBytesBackend();
    }

    /**
     * Creates VisionTemplate bean configured with FaceBytesBackend if not already defined.
     *
     * @param backend the FaceBytes backend
     * @return configured VisionTemplate instance
     */
    @Bean
    @ConditionalOnMissingBean(name = "faceBytesVisionTemplate")
    public VisionTemplate faceBytesVisionTemplate(FaceBytesBackend backend) {
        return new VisionTemplate(backend);
    }
}
