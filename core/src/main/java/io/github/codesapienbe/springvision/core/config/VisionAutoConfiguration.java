package io.github.codesapienbe.springvision.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.backend.OpenCvVisionBackend;

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
 * @see VisionProperties
 * @see VisionHealthIndicator
 * @see VisionMetrics
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VisionProperties.class)
public class VisionAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(VisionAutoConfiguration.class);

    private final Environment environment;

    // Support both legacy "vision.*" and unified "spring.vision.*" prefixes
    @Value("${spring.vision.enabled:#{null}}")
    private Boolean springVisionEnabled;

    @Value("${spring.vision.fail-fast:#{null}}")
    private Boolean springVisionFailFast;

    @Value("${spring.vision.backend:#{null}}")
    private String springVisionBackend;

    public VisionAutoConfiguration(Environment environment) {
        this.environment = environment;
        logger.info("VisionAutoConfiguration constructor called - auto-configuration is being loaded");
    }

    /**
     * Creates the primary vision backend when failFast is enabled (default).
     * Fails application startup if backend initialization fails.
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(VisionBackend.class)
    @ConditionalOnExpression("(${spring.vision.enabled:true} and ${spring.vision.fail-fast:true}) or (${vision.enabled:true} and ${vision.fail-fast:true})")
    public VisionBackend visionBackendFailFast(VisionProperties properties) {
        logger.info("=== VisionAutoConfiguration: Creating VisionBackend bean (fail-fast mode) ===");

        // Determine effective settings, preferring spring.vision.* if provided
        boolean enabled = springVisionEnabled != null ? springVisionEnabled : properties.isEnabled();
        boolean failFast = springVisionFailFast != null ? springVisionFailFast : properties.isFailFast();
        String configuredBackend = (springVisionBackend != null && !springVisionBackend.isBlank())
            ? springVisionBackend
            : properties.getBackend();

        logger.info("Vision enabled={}, failFast={}, backend={}", enabled, failFast, configuredBackend);

        // Normalize backend name, defaulting to opencv if null or empty
        String backendName = (configuredBackend == null || configuredBackend.trim().isEmpty())
            ? "opencv"
            : configuredBackend.trim().toLowerCase();

        return createBackendByName(backendName, properties);
    }

    /**
     * Skips vision backend creation when failFast is disabled.
     * This allows applications to provide their own fallback implementations.
     */
    @ConditionalOnExpression("(${spring.vision.enabled:true} and !${spring.vision.fail-fast:true}) or (${vision.enabled:true} and !${vision.fail-fast:true})")
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
     * @param backend    the vision backend to monitor
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
     * @param backend       the vision backend to monitor
     * @param meterRegistry the meter registry for metrics collection
     * @param properties    the vision configuration properties
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
     * Create a vision backend by name, using reflection for optional modules to avoid hard dependencies.
     * Falls back to OpenCV when the requested backend isn't on classpath.
     */
    private VisionBackend createBackendByName(String backendName, VisionProperties properties) {
        return switch (backendName) {
            case "opencv" -> {
                logger.info("Initializing OpenCV backend (default)");
                yield createOpenCvBackend(properties);
            }
            case "mediapipe" ->
                reflectInstantiate("io.github.codesapienbe.springvision.mediapipe.MediaPipeBackend", properties);
            case "yolo" -> reflectInstantiate("io.github.codesapienbe.springvision.yolo.YoloBackend", properties);
            case "deepface" ->
                reflectInstantiate("io.github.codesapienbe.springvision.deepface.DeepFaceBackend", properties);
            case "insightface" ->
                reflectInstantiate("io.github.codesapienbe.springvision.insightface.InsightFaceBackend", properties);
            case "compreface" ->
                reflectInstantiate("io.github.codesapienbe.springvision.compreface.CompreFaceBackend", properties);
            case "tesseract" ->
                reflectInstantiate("io.github.codesapienbe.springvision.tesseract.TesseractVisionBackend", properties);
            case "cyber" ->
                reflectInstantiate("io.github.codesapienbe.springvision.cyber.CyberSecurityBackend", properties);
            default -> {
                logger.warn("Backend '{}' not supported or not on classpath. Falling back to OpenCV.", backendName);
                yield createOpenCvBackend(properties);
            }
        };
    }

    private VisionBackend reflectInstantiate(String className, VisionProperties properties) {
        try {
            Class<?> clazz = Class.forName(className);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            logger.info("Initialized backend via reflection: {}", className);

            // Attempt to bind configuration properties for this backend
            bindBackendProperties(instance);

            return (VisionBackend) instance;
        } catch (Throwable t) {
            logger.warn("Could not initialize backend '{}': {}. Falling back to OpenCV.", className, t.toString());
            return createOpenCvBackend(properties);
        }
    }

    /**
     * Binds spring.vision.<module> properties to the given backend instance using Spring Boot Binder.
     */
    private void bindBackendProperties(Object backend) {
        try {
            String backendId = backend.getClass().getSimpleName().toLowerCase();
            String prefix = resolvePropertiesPrefix(backend);
            if (prefix == null) {
                logger.debug("No properties prefix resolved for backend class: {}", backend.getClass().getName());
                return;
            }
            Binder binder = Binder.get(environment);
            binder.bind(prefix, Bindable.ofInstance(backend));
            logger.info("Bound properties for backend: prefix={} -> beanClass={}", prefix, backend.getClass().getName());
        } catch (Exception e) {
            logger.debug("Property binding skipped/failed for backend {}: {}", backend.getClass().getName(), e.getMessage());
        }
    }

    /**
     * Resolve the configuration properties prefix for a backend instance.
     */
    private String resolvePropertiesPrefix(Object backend) {
        String pkg = backend.getClass().getPackageName();
        if (pkg.contains("mediapipe")) return "spring.vision.mediapipe";
        if (pkg.contains("yolo")) return "spring.vision.yolo";
        if (pkg.contains("deepface")) return "spring.vision.deepface";
        if (pkg.contains("insightface")) return "spring.vision.insightface";
        if (pkg.contains("compreface")) return "spring.vision.compreface";
        if (pkg.contains("tesseract")) return "spring.vision.tesseract";
        if (pkg.contains("cyber")) return "spring.vision.cyber";
        if (pkg.contains("opencv")) return "spring.vision.opencv";
        return null;
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
                    "choose a different backend with 'spring.vision.backend=mediapipe'. Error: %s",
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
