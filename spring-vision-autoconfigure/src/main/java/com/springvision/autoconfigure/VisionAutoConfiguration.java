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
import com.springvision.core.backend.StableOpenCvVisionBackend;
import com.springvision.core.backend.MediaPipeVisionBackend;
import com.springvision.core.backend.DeepFaceBackend;

import io.micrometer.core.instrument.MeterRegistry;

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
     * Creates the primary vision backend based on configuration.
     *
     * <p>
     * This method creates the vision backend specified in the configuration.
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
        logger.info("=== VisionAutoConfiguration: Creating VisionBackend bean ===");
        logger.info("Configuring vision backend: {}", properties.getBackend());

        return switch (properties.getBackend().toLowerCase()) {
            case "opencv" ->
                createOpenCvBackend(properties);
            case "mediapipe" ->
                createMediaPipeBackend(properties);
            case "yolo" ->
                createYoloBackend(properties);
            case "deepface" ->
                createDeepFaceBackend(properties);
            default -> {
                logger.warn("Unknown backend '{}'", properties.getBackend());
                yield createOpenCvBackend(properties);
            }
        };
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
     */
        private VisionBackend createOpenCvBackend(VisionProperties properties) {
        logger.info("Creating OpenCV vision backend");

        try {
            // Try direct instantiation first with stability wrapper
            StableOpenCvVisionBackend backend = new StableOpenCvVisionBackend(new OpenCvVisionBackend());
            backend.initialize();
            logger.info("OpenCV backend initialized successfully (stable wrapper)");
            return backend;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError | ExceptionInInitializerError | Exception e) {
            logger.warn("Failed to create or initialize OpenCV backend - using OpenCV with degraded functionality: {}", e.getMessage());
            return createOpenCvBackendWithDegradedFunctionality(e);
        }
    }

    /**
     * Creates an OpenCV vision backend with degraded functionality.
     *
     * <p>
     * This method creates a VisionBackend implementation that provides
     * minimal functionality due to a failed initialization attempt.</p>
     *
     * @param initializationError the error that occurred during OpenCV initialization
     * @return a VisionBackend implementation with degraded functionality
     */
    private VisionBackend createOpenCvBackendWithDegradedFunctionality(Throwable initializationError) {
        logger.info("Creating OpenCV backend with degraded functionality");

        return new VisionBackend() {
            private static final String BACKEND_ID = "opencv";
            private static final String DISPLAY_NAME = "OpenCV Vision Backend (Degraded)";
            private static final String VERSION = "4.8.1";

            @Override
            public String getBackendId() {
                return BACKEND_ID;
            }

            @Override
            public String getDisplayName() {
                return DISPLAY_NAME;
            }

            @Override
            public String getVersion() {
                return VERSION;
            }

            @Override
            public java.util.Set<com.springvision.core.DetectionType> getSupportedDetectionTypes() {
                return java.util.Set.of(com.springvision.core.DetectionType.FACE, com.springvision.core.DetectionType.OBJECT);
            }

            @Override
            public boolean isHealthy() {
                return false; // OpenCV backend is not healthy due to initialization failure
            }

            @Override
            public com.springvision.core.BackendHealthInfo getHealthInfo() {
                return com.springvision.core.BackendHealthInfo.unhealthy(
                    BACKEND_ID,
                    "OpenCV backend - degraded functionality",
                    "OpenCV backend failed to initialize: " + initializationError.getMessage(),
                    0
                );
            }

            @Override
            public com.springvision.core.VisionResult detectFaces(com.springvision.core.ImageData imageData) {
                logger.warn("OpenCV backend: Face detection not available - OpenCV failed to initialize");
                throw new com.springvision.core.exception.VisionBackendException(
                    "Face detection not available - OpenCV backend failed to initialize: " + initializationError.getMessage(),
                    "opencv_initialization_failed",
                    null,
                    initializationError
                );
            }

            @Override
            public com.springvision.core.VisionResult detectObjects(com.springvision.core.ImageData imageData) {
                logger.warn("OpenCV backend: Object detection not available - OpenCV failed to initialize");
                throw new com.springvision.core.exception.VisionBackendException(
                    "Object detection not available - OpenCV backend failed to initialize: " + initializationError.getMessage(),
                    "opencv_initialization_failed",
                    null,
                    initializationError
                );
            }
        };
    }

    /**
     * Creates a MediaPipe vision backend with the specified configuration.
     *
     * <p>
     * This is a placeholder for future MediaPipe integration. Currently throws
     * an UnsupportedOperationException.</p>
     *
     * @param properties the vision configuration properties
     * @return the configured MediaPipe backend
     * @throws UnsupportedOperationException as MediaPipe is not yet implemented
     */
    private VisionBackend createMediaPipeBackend(VisionProperties properties) {
        logger.info("Creating MediaPipe vision backend");
        try {
            MediaPipeVisionBackend backend = new MediaPipeVisionBackend();
            backend.initialize();
            if (!backend.isHealthy()) {
                logger.warn("MediaPipe backend initialized but unhealthy: {}", backend.getHealthInfo().errorMessage());
            }
            return backend;
        } catch (UnsupportedOperationException e) {
            logger.warn("MediaPipe backend is not yet implemented");
            throw e;
        } catch (Exception e) {
            logger.error("Failed to create or initialize MediaPipe backend: {}", e.getMessage(), e);
            throw new UnsupportedOperationException("MediaPipe backend initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a YOLO vision backend with the specified configuration.
     *
     * <p>
     * This is a placeholder for future YOLO integration. Currently throws an
     * UnsupportedOperationException.</p>
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
     * Creates a DeepFace backend that forwards requests to an external REST API.
     *
     * @param properties the vision configuration properties
     * @return the configured DeepFace backend
     */
    private VisionBackend createDeepFaceBackend(VisionProperties properties) {
        logger.info("Creating DeepFace REST backend");
        VisionProperties.DeepFace cfg = properties.getDeepface();
        DeepFaceBackend backend = new DeepFaceBackend(
                cfg.getBaseUrl(),
                cfg.getModelName(),
                cfg.getDetectorBackend(),
                cfg.getDistanceMetric(),
                cfg.getConnectTimeout(),
                cfg.getReadTimeout()
        );
        try {
            backend.initialize();
        } catch (Exception e) {
            logger.warn("DeepFace backend initialization encountered an error: {}", e.getMessage());
        }
        if (!backend.isHealthy()) {
            logger.warn("DeepFace backend appears unhealthy: {}", backend.getHealthInfo().errorMessage());
        }
        return backend;
    }
}
