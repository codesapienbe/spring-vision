package io.github.codesapienbe.springvision.core.djl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for DJL vision backend.
 *
 * <p>Enables DJL-based model loading and inference when configured.</p>
 *
 * <p>Example configuration:</p>
 * <pre>
 * spring.vision.djl.enabled=true
 * spring.vision.djl.engine=PyTorch
 * spring.vision.djl.use-gpu=false
 * spring.vision.djl.confidence-threshold=0.5
 * </pre>
 *
 * @author Spring Vision Team
 * @since 1.0.5
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.vision.djl", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DjlProperties.class)
public class DjlAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DjlAutoConfiguration.class);

    public DjlAutoConfiguration() {
        logger.info("DJL Vision Backend auto-configuration enabled");
    }

    @Bean
    public DjlVisionBackend djlVisionBackend(DjlProperties properties) {
        logger.info("Creating DJL vision backend with engine: {}", properties.getEngine());

        // Optional: set DJL cache / model zoo location
        if (properties.getCacheDir() != null) {
            System.setProperty("ai.djl.repository.cache.dir", properties.getCacheDir());
        }
        if (properties.getModelZooLocation() != null) {
            System.setProperty("ai.djl.repository.zoo.location", properties.getModelZooLocation());
        }

        return new DjlVisionBackend(properties);
    }
}
