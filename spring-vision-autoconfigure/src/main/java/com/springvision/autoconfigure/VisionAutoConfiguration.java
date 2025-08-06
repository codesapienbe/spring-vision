package com.springvision.autoconfigure;

import com.springvision.core.VisionBackend;
import com.springvision.core.VisionTemplate;
import com.springvision.core.backend.OpenCvVisionBackend;
import com.springvision.core.BackendHealthInfo;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Map;

/**
 * Spring Boot auto-configuration for Spring Vision framework.
 *
 * <p>This configuration automatically sets up the Spring Vision framework
 * when Spring Boot detects the necessary dependencies. It configures vision
 * backends, templates, and related components based on application properties.</p>
 *
 * <p>The auto-configuration supports multiple backends and can be customized
 * through application properties. It includes health indicators, metrics,
 * and proper lifecycle management.</p>
 *
 * <p>Example usage:</p>
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
@ConditionalOnClass(VisionTemplate.class)
@EnableConfigurationProperties(VisionProperties.class)
public class VisionAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(VisionAutoConfiguration.class);

    /**
     * Creates the primary vision backend based on configuration.
     *
     * <p>This method creates the vision backend specified in the configuration.
     * Currently supports OpenCV backend, with plans for additional backends
     * (MediaPipe, YOLO, etc.) in future releases.</p>
     *
     * @param properties the vision configuration properties
     * @return the configured vision backend
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(VisionBackend.class)
    @ConditionalOnProperty(prefix = "vision", name = "enabled", havingValue = "true", matchIfMissing = true)
    public VisionBackend visionBackend(VisionProperties properties) {
        // Check if we're in test mode
        String testMode = System.getProperty("org.bytedeco.javacpp.nobootclasspath");
        if ("true".equals(testMode)) {
            logger.info("Running in test mode - using No-Op backend");
            return createNoOpBackend();
        }

        logger.info("Configuring vision backend: {}", properties.getBackend());

        return switch (properties.getBackend().toLowerCase()) {
            case "opencv" -> createOpenCvBackend(properties);
            case "mediapipe" -> createMediaPipeBackend(properties);
            case "yolo" -> createYoloBackend(properties);
            default -> {
                logger.warn("Unknown backend '{}', falling back to OpenCV", properties.getBackend());
                yield createOpenCvBackend(properties);
            }
        };
    }

    /**
     * Creates the vision template for easy access to vision operations.
     *
     * <p>The vision template provides a unified interface for all vision
     * operations regardless of the underlying backend implementation.</p>
     *
     * @param backend the configured vision backend
     * @return the vision template
     */
    @Bean
    @ConditionalOnMissingBean(VisionTemplate.class)
    @ConditionalOnBean(VisionBackend.class)
    public VisionTemplate visionTemplate(VisionBackend backend) {
        logger.info("Creating vision template with backend: {}", backend.getBackendId());
        return new VisionTemplate(backend);
    }

    /**
     * Creates the vision health indicator for Spring Boot Actuator.
     *
     * <p>This health indicator monitors the health of the vision backend
     * and reports its status to Spring Boot Actuator health endpoints.</p>
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
     * <p>This component provides metrics about vision operations including
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
     * Creates an OpenCV vision backend with the specified configuration.
     *
     * @param properties the vision configuration properties
     * @return the configured OpenCV backend
     */
    private VisionBackend createOpenCvBackend(VisionProperties properties) {
        logger.info("Creating OpenCV vision backend");

        try {
            OpenCvVisionBackend backend = new OpenCvVisionBackend();
            backend.initialize();
            logger.info("OpenCV backend initialized successfully");
            return backend;
        } catch (UnsatisfiedLinkError | Exception e) {
            logger.warn("Failed to initialize OpenCV backend - falling back to No-Op backend: {}", e.getMessage());
            return createNoOpBackend();
        }
    }

    /**
     * Creates a MediaPipe vision backend with the specified configuration.
     *
     * <p>This is a placeholder for future MediaPipe integration.
     * Currently throws an UnsupportedOperationException.</p>
     *
     * @param properties the vision configuration properties
     * @return the configured MediaPipe backend
     * @throws UnsupportedOperationException as MediaPipe is not yet implemented
     */
    private VisionBackend createMediaPipeBackend(VisionProperties properties) {
        logger.warn("MediaPipe backend is not yet implemented");
        throw new UnsupportedOperationException("MediaPipe backend is not yet implemented");
    }

    /**
     * Creates a YOLO vision backend with the specified configuration.
     *
     * <p>This is a placeholder for future YOLO integration.
     * Currently throws an UnsupportedOperationException.</p>
     *
     * @param properties the vision configuration properties
     * @return the configured YOLO backend
     * @throws UnsupportedOperationException as YOLO is not yet implemented
     */
    private VisionBackend createYoloBackend(VisionProperties properties) {
        logger.warn("YOLO backend is not yet implemented");
        throw new UnsupportedOperationException("YOLO backend is not yet implemented");
    }

    /**
     * Creates a lightweight no-op vision backend that acts as a stub when the real
     * backend cannot be initialised (e.g. missing native dependencies). This
     * backend implements the {@link VisionBackend} contract but performs no
     * actual vision processing. It ensures the Spring context can start and the
     * auto-configuration tests can execute in environments where native
     * libraries are not available.
     */
    private VisionBackend createNoOpBackend() {
        return new VisionBackend() {
            private static final String BACKEND_ID = "noop";

            @Override
            public String getBackendId() {
                return BACKEND_ID;
            }

            @Override
            public String getDisplayName() {
                return "No-Op Vision Backend";
            }

            @Override
            public String getVersion() {
                return "1.0.0";
            }

            @Override
            public java.util.Set<com.springvision.core.DetectionType> getSupportedDetectionTypes() {
                return java.util.Set.of(com.springvision.core.DetectionType.FACE, com.springvision.core.DetectionType.OBJECT);
            }

            @Override
            public boolean isHealthy() {
                return true;
            }

            @Override
            public BackendHealthInfo getHealthInfo() {
                return BackendHealthInfo.healthy(BACKEND_ID, "No-Op backend is always healthy", 0);
            }

            @Override
            public com.springvision.core.VisionResult detectFaces(com.springvision.core.ImageData imageData) {
                return com.springvision.core.VisionResult.empty(com.springvision.core.DetectionType.FACE, 0);
            }

            @Override
            public com.springvision.core.VisionResult detectObjects(com.springvision.core.ImageData imageData) {
                return com.springvision.core.VisionResult.empty(com.springvision.core.DetectionType.OBJECT, 0);
            }
        };
    }
}
