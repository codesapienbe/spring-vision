package io.github.codesapienbe.springvision.deepface.config;

import io.github.codesapienbe.springvision.deepface.DeepFaceBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Spring Vision DeepFace module.
 *
 * <p>This configuration is activated when:
 * <ul>
 *   <li>spring.vision.deepface.enabled=true</li>
 *   <li>DeepFaceBackend class is on the classpath</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(DeepFaceBackend.class)
@ConditionalOnProperty(prefix = "spring.vision.deepface", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DeepFaceProperties.class)
public class DeepFaceAutoConfiguration {

    /**
     * Default constructor for {@link DeepFaceAutoConfiguration}.
     */
    public DeepFaceAutoConfiguration() {
        // Default constructor
    }

    /**
     * Creates DeepFaceBackend bean if not already defined.
     *
     * @param properties the DeepFace configuration properties
     * @return configured DeepFaceBackend instance
     */
    @Bean
    @ConditionalOnMissingBean
    public DeepFaceBackend deepFaceBackend(DeepFaceProperties properties) {
        return new DeepFaceBackend(properties.baseUrl(), properties.timeout());
    }

    /**
     * Creates VisionTemplate bean configured with DeepFaceBackend if not already defined.
     *
     * @param backend the DeepFace backend
     * @return configured VisionTemplate instance
     */
    @Bean
    @ConditionalOnMissingBean(name = "deepFaceVisionTemplate")
    public VisionTemplate deepFaceVisionTemplate(DeepFaceBackend backend) {
        return new VisionTemplate(backend);
    }
}
