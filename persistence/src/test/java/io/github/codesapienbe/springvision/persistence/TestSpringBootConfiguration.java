package io.github.codesapienbe.springvision.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Minimal test Spring Boot configuration for the JPA module.
 * <p>
 * This provides a simple application context used by integration tests
 * annotated with @SpringBootTest in this module.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "io.github.codesapienbe.springvision.jpa")
@EntityScan(basePackages = "io.github.codesapienbe.springvision.persistence.entity")
@EnableTransactionManagement
public class TestSpringBootConfiguration {

    @org.springframework.context.annotation.Bean
    public io.github.codesapienbe.springvision.core.VisionBackend testVisionBackend() {
        // Provide a simple OpenCV backend instance for tests so auto-configurations
        // that require a VisionBackend bean can proceed. The backend is not
        // initialized here; tests can choose to initialize it if needed.
        return new io.github.codesapienbe.springvision.core.backend.OpenCvVisionBackend();
    }
}
