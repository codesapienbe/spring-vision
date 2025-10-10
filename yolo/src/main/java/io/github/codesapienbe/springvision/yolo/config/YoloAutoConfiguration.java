package io.github.codesapienbe.springvision.yolo.config;

import io.github.codesapienbe.springvision.yolo.YoloBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Spring Vision YOLO module.
 *
 * <p>This configuration is activated when:
 * <ul>
 *   <li>spring.vision.yolo.enabled=true</li>
 *   <li>YoloBackend class is on the classpath</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
@AutoConfiguration
@ConditionalOnClass(YoloBackend.class)
@ConditionalOnProperty(prefix = "spring.vision.yolo", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(YoloProperties.class)
public class YoloAutoConfiguration {

    /**
     * Creates YoloBackend bean if not already defined.
     *
     * @param properties the YOLO configuration properties
     * @return configured YoloBackend instance
     */
    @Bean
    @ConditionalOnMissingBean
    public YoloBackend yoloBackend(YoloProperties properties) {
        return new YoloBackend();
    }

    /**
     * Creates VisionTemplate bean configured with YoloBackend if not already defined.
     *
     * @param backend the YOLO backend
     * @return configured VisionTemplate instance
     */
    @Bean
    @ConditionalOnMissingBean(name = "yoloVisionTemplate")
    public VisionTemplate yoloVisionTemplate(YoloBackend backend) {
        return new VisionTemplate(backend);
    }
}
