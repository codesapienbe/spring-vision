package io.github.codesapienbe.springvision.mediapipe.config;

import io.github.codesapienbe.springvision.mediapipe.MediaPipeBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Spring Vision MediaPipe module.
 *
 * <p>This configuration is activated when:
 * <ul>
 *   <li>spring.vision.mediapipe.enabled=true</li>
 *   <li>MediaPipeBackend class is on the classpath</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
@AutoConfiguration
@ConditionalOnClass(MediaPipeBackend.class)
@ConditionalOnProperty(prefix = "spring.vision.mediapipe", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MediaPipeProperties.class)
public class MediaPipeAutoConfiguration {

    /**
     * Creates MediaPipeBackend bean if not already defined.
     *
     * @param properties the MediaPipe configuration properties
     * @return configured MediaPipeBackend instance
     */
    @Bean
    @ConditionalOnMissingBean
    public MediaPipeBackend mediaPipeBackend(MediaPipeProperties properties) {
        return new MediaPipeBackend();
    }

    /**
     * Creates VisionTemplate bean configured with MediaPipeBackend if not already defined.
     *
     * @param backend the MediaPipe backend
     * @return configured VisionTemplate instance
     */
    @Bean
    @ConditionalOnMissingBean(name = "mediaPipeVisionTemplate")
    public VisionTemplate mediaPipeVisionTemplate(MediaPipeBackend backend) {
        return new VisionTemplate(backend);
    }
}

