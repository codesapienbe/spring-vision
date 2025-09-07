package com.springvision.jpa;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Minimal test Spring Boot configuration for the JPA module.
 *
 * This provides a simple application context used by integration tests
 * annotated with @SpringBootTest in this module.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.springvision.jpa")
@EntityScan(basePackages = "com.springvision.jpa.entity")
@EnableTransactionManagement
public class TestSpringBootConfiguration {

    @org.springframework.context.annotation.Bean
    public com.springvision.core.VisionBackend testVisionBackend() {
        // Provide a simple OpenCV backend instance for tests so auto-configurations
        // that require a VisionBackend bean can proceed. The backend is not
        // initialized here; tests can choose to initialize it if needed.
        return new com.springvision.core.backend.OpenCvVisionBackend();
    }
} 