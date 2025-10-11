package io.github.codesapienbe.springvision.robotics.config;

import io.github.codesapienbe.springvision.robotics.RoboticsBackend;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the Spring Vision Robotics module.
 *
 * <p>This configuration class sets up the robotics backend when enabled through
 * configuration properties. It provides beans for industrial automation and
 * robotics-focused computer vision capabilities.</p>
 *
 * @author Spring Vision Team
 * @since 2.0.0
 */
@Configuration
@EnableConfigurationProperties(RoboticsProperties.class)
@ConditionalOnProperty(prefix = "spring.vision.robotics", name = "enabled", havingValue = "true", matchIfMissing = false)
public class RoboticsAutoConfiguration {

    /**
     * Creates the RoboticsBackend bean if not already present.
     *
     * @param properties the robotics configuration properties
     * @return configured RoboticsBackend instance
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.vision.robotics", name = "enabled", havingValue = "true")
    public RoboticsBackend roboticsBackend(RoboticsProperties properties) {
        return new RoboticsBackend();
    }
}

