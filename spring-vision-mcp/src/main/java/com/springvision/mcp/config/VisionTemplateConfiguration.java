package com.springvision.mcp.config;

import com.springvision.core.VisionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal configuration for MCP module to expose a VisionTemplate bean
 * so controllers and tests can autowire it. This keeps the default
 * behavior of using the OpenCV backend for local tests/examples.
 */
@Configuration
public class VisionTemplateConfiguration {

    @Bean
    public VisionTemplate visionTemplate() {
        // Use the default no-arg constructor which wires OpenCV for tests
        return new VisionTemplate();
    }
}

