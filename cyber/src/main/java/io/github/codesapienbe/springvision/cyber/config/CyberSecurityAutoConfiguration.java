package io.github.codesapienbe.springvision.cyber.config;

import io.github.codesapienbe.springvision.cyber.CyberSecurityBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the Cyber Security vision backend.
 *
 * <p>This configuration class sets up the cyber security backend when enabled through
 * configuration properties. It provides beans for security-focused computer vision capabilities.</p>
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
@AutoConfiguration
@ConditionalOnClass(CyberSecurityBackend.class)
@ConditionalOnProperty(prefix = "spring.vision.cyber", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CyberSecurityProperties.class)
public class CyberSecurityAutoConfiguration {

    /**
     * Default constructor for {@link CyberSecurityAutoConfiguration}.
     */
    public CyberSecurityAutoConfiguration() {
        // Default constructor
    }

    /**
     * Creates CyberSecurityBackend bean if not already defined.
     *
     * @param properties the Cyber Security configuration properties
     * @return configured CyberSecurityBackend instance
     */
    @Bean
    @ConditionalOnMissingBean
    public CyberSecurityBackend cyberSecurityBackend(CyberSecurityProperties properties) {
        return new CyberSecurityBackend();
    }

    /**
     * Creates VisionTemplate bean configured with CyberSecurityBackend if not already defined.
     *
     * @param backend the Cyber Security backend
     * @return configured VisionTemplate instance
     */
    @Bean
    @ConditionalOnMissingBean(name = "cyberSecurityVisionTemplate")
    public VisionTemplate cyberSecurityVisionTemplate(CyberSecurityBackend backend) {
        return new VisionTemplate(backend);
    }

}
