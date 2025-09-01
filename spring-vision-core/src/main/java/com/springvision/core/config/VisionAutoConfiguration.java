package com.springvision.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

import com.springvision.core.VisionBackend;
import com.springvision.core.VisionTemplate;
import com.springvision.core.backend.OpenCvVisionBackend;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

/**
 * Spring Boot auto-configuration for Spring Vision framework.
 *
 * <p>
 * This configuration automatically sets up the Spring Vision framework when
 * Spring Boot detects the necessary dependencies. It configures vision
 * backends, templates, and related components based on application
 * properties.</p>
 *
 * <p>
 * The auto-configuration supports multiple backends and can be customized
 * through application properties. It includes health indicators, metrics, and
 * proper lifecycle management.</p>
 *
 * <p>
 * <strong>Backend Selection:</strong> If no backend is explicitly configured via
 * {@code vision.backend} property, OpenCV will be used as the default backend.
 * Supported backends include: opencv (default), mediapipe, yolo, deepface.</p>
 *
 * <p>
 * Example usage:</p>
 * <pre>{@code
 * @SpringBootApplication
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 *
 * // VisionTemplate will be automatically configured and available for injection
 * @Autowired
 * private VisionTemplate visionTemplate;
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionProperties
 * @see VisionHealthIndicator
 * @see VisionMetrics
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VisionProperties.class)
public class VisionAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(VisionAutoConfiguration.class);

    public VisionAutoConfiguration() {
        logger.info("VisionAutoConfiguration constructor called - auto-configuration is being loaded");
    }

    /**
     * Creates the primary vision backend when failFast is enabled (default).
     * Fails application startup if backend initialization fails.
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(VisionBackend.class)
    @ConditionalOnExpression("${vision.enabled:true} && ${vision.fail-fast:true}")
    public VisionBackend visionBackendFailFast(VisionProperties properties) {
        logger.info("=== VisionAutoConfiguration: Creating VisionBackend bean (fail-fast mode) ===");
        
        // Normalize backend name, defaulting to opencv if null or empty
        String backendName = properties.getBackend();
        if (backendName == null || backendName.trim().isEmpty()) {
            backendName = "opencv";
            logger.info("No backend specified, using default: opencv");
        } else {
            logger.info("Configuring vision backend: {}", backendName);
        }

        return switch (backendName.toLowerCase().trim()) {
            case "opencv" -> {
                logger.info("Initializing OpenCV backend (default)");
                yield createOpenCvBackend(properties);
            }
            default -> {
                logger.warn("Backend '{}' not supported in core module. Use OpenCV (default) or add the appropriate backend module.", backendName);
                yield createOpenCvBackend(properties);
            }
        };
    }

    /**
     * Skips vision backend creation when failFast is disabled.
     * This allows applications to provide their own fallback implementations.
     */
    @ConditionalOnExpression("${vision.enabled:true} && !${vision.fail-fast:true}")
    @ConditionalOnMissingBean(VisionBackend.class)
    public void skipVisionBackendCreation() {
        logger.info("=== VisionAutoConfiguration: Skipping VisionBackend creation (fail-fast=false) ===");
        logger.info("Application may provide its own VisionBackend implementation as fallback");
    }

    /**
     * Creates the vision template for easy access to vision operations.
     *
     * <p>
     * The vision template provides a unified interface for all vision
     * operations regardless of the underlying backend implementation.</p>
     *
     * @param backend the configured vision backend
     * @return the vision template
     */
    @Bean
    @ConditionalOnMissingBean(VisionTemplate.class)
    @ConditionalOnBean(VisionBackend.class)
    public VisionTemplate visionTemplate(VisionBackend backend) {
        logger.info("=== VisionAutoConfiguration: Creating VisionTemplate bean ===");
        logger.info("Creating vision template with backend: {}", backend.getBackendId());
        return new VisionTemplate(backend);
    }

    /**
     * Creates the vision health indicator for Spring Boot Actuator.
     *
     * <p>
     * This health indicator monitors the health of the vision backend and
     * reports its status to Spring Boot Actuator health endpoints.</p>
     *
     * @param backend the vision backend to monitor
     * @param properties the vision configuration properties
     * @return the vision health indicator
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuator.health.HealthIndicator")
    @ConditionalOnMissingBean(VisionHealthIndicator.class)
    @ConditionalOnProperty(prefix = "vision.health", name = "enabled", havingValue = "true", matchIfMissing = true)
    public VisionHealthIndicator visionHealthIndicator(VisionBackend backend, VisionProperties properties) {
        logger.info("Creating vision health indicator");
        return new VisionHealthIndicator(backend, properties);
    }

    /**
     * Creates the vision metrics for monitoring and observability.
     *
     * <p>
     * This component provides metrics about vision operations including
     * detection counts, processing times, and error rates.</p>
     *
     * @param backend the vision backend to monitor
     * @param meterRegistry the meter registry for metrics collection
     * @param properties the vision configuration properties
     * @return the vision metrics
     */
    @Bean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnMissingBean(VisionMetrics.class)
    @ConditionalOnProperty(prefix = "vision.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(VisionBackend.class)
    public VisionMetrics visionMetrics(VisionBackend backend, MeterRegistry meterRegistry, VisionProperties properties) {
        logger.info("Creating vision metrics with backend: {}", backend.getClass().getSimpleName());
        return new VisionMetrics(backend, meterRegistry, properties);
    }

    /**
     * Application context shutdown hook.
     */
    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
    }

    /**
     * Creates an OpenCV vision backend with the specified configuration.
     *
     * @param properties the vision configuration properties
     * @return the configured OpenCV backend
     * @throws IllegalStateException if OpenCV backend cannot be initialized
     */
    private VisionBackend createOpenCvBackend(VisionProperties properties) {
        logger.info("Creating OpenCV vision backend");

        try {
            OpenCvVisionBackend backend = new OpenCvVisionBackend();
            backend.initialize();
            logger.info("OpenCV backend initialized successfully");
            return backend;
        } catch (UnsatisfiedLinkError e) {
            String errorMessage = String.format(
                "OpenCV native libraries not found. Please ensure OpenCV is properly installed. " +
                "You can disable vision auto-configuration with 'vision.enabled=false' or " +
                "choose a different backend with 'vision.backend=mediapipe'. Error: %s", 
                e.getMessage()
            );
            logger.error(errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        } catch (NoClassDefFoundError e) {
            String errorMessage = String.format(
                "OpenCV Java bindings not found on classpath. Please add opencv-platform dependency. " +
                "You can disable vision auto-configuration with 'vision.enabled=false'. Error: %s", 
                e.getMessage()
            );
            logger.error(errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        } catch (ExceptionInInitializerError e) {
            String errorMessage = String.format(
                "OpenCV initialization failed during static initialization. " +
                "This usually indicates a native library compatibility issue. Error: %s", 
                e.getMessage()
            );
            logger.error(errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = String.format(
                "Unexpected error during OpenCV backend initialization. " +
                "Please check your OpenCV installation and configuration. Error: %s", 
                e.getMessage()
            );
            logger.error(errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        }
    }

}
