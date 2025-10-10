package io.github.codesapienbe.springvision.insightface.config;

import io.github.codesapienbe.springvision.insightface.InsightFaceBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Spring Vision InsightFace module.
 *
 * <p>This configuration is activated when:
 * <ul>
 *   <li>spring.vision.insightface.enabled=true</li>
 *   <li>InsightFaceBackend class is on the classpath</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
@AutoConfiguration
@ConditionalOnClass(InsightFaceBackend.class)
@ConditionalOnProperty(prefix = "spring.vision.insightface", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(InsightFaceProperties.class)
public class InsightFaceAutoConfiguration {

    /**
     * Creates InsightFaceBackend bean if not already defined.
     *
     * @param properties the InsightFace configuration properties
     * @return configured InsightFaceBackend instance
     */
    @Bean
    @ConditionalOnMissingBean
    public InsightFaceBackend insightFaceBackend(InsightFaceProperties properties) {
        return new InsightFaceBackend();
    }

    /**
     * Creates VisionTemplate bean configured with InsightFaceBackend if not already defined.
     *
     * @param backend the InsightFace backend
     * @return configured VisionTemplate instance
     */
    @Bean
    @ConditionalOnMissingBean(name = "insightFaceVisionTemplate")
    public VisionTemplate insightFaceVisionTemplate(InsightFaceBackend backend) {
        return new VisionTemplate(backend);
    }
}

