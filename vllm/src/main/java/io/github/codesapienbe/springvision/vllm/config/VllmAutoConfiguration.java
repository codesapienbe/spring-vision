package io.github.codesapienbe.springvision.vllm.config;

import io.github.codesapienbe.springvision.vllm.GraniteVisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Spring Vision vLLM module.
 *
 * <p>This configuration is activated when:
 * <ul>
 *   <li>spring.vision.vllm.enabled=true</li>
 *   <li>GraniteVisionBackend class is on the classpath</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(GraniteVisionBackend.class)
@ConditionalOnProperty(prefix = "spring.vision.vllm", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(VllmProperties.class)
public class VllmAutoConfiguration {

    /**
     * Default constructor for {@link VllmAutoConfiguration}.
     */
    public VllmAutoConfiguration() {
        // Default constructor
    }

    /**
     * Creates GraniteVisionBackend bean if not already defined.
     *
     * @param properties the vLLM configuration properties
     * @return configured GraniteVisionBackend instance
     */
    @Bean
    @ConditionalOnMissingBean
    public GraniteVisionBackend graniteVisionBackend(VllmProperties properties) {
        return new GraniteVisionBackend(properties);
    }

    /**
     * Creates VisionTemplate bean configured with GraniteVisionBackend if not already defined.
     *
     * @param backend the Granite vision backend
     * @return configured VisionTemplate instance
     */
    @Bean
    @ConditionalOnMissingBean(name = "graniteVisionTemplate")
    public VisionTemplate graniteVisionTemplate(GraniteVisionBackend backend) {
        return new VisionTemplate(backend);
    }
}

