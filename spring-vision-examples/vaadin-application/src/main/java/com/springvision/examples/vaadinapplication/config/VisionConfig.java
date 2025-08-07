package com.springvision.examples.vaadinapplication.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Vaadin application.
 *
 * <p>This configuration class provides Vaadin-specific configuration and ensures
 * proper integration with the Spring Vision framework. The VisionTemplate bean
 * is automatically configured by the Spring Vision autoconfiguration.</p>
 *
 * <p>This configuration follows the same pattern as the GWT application to ensure
 * consistent behavior across different frontend technologies.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Configuration
public class VisionConfig {

    private static final Logger logger = LoggerFactory.getLogger(VisionConfig.class);

    /**
     * Constructor that logs the configuration initialization.
     */
    public VisionConfig() {
        logger.info("Vaadin application VisionConfig initialized - using autoconfigured VisionTemplate");
    }
}
