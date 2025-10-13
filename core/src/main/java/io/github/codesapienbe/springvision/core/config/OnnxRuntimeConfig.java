package io.github.codesapienbe.springvision.core.config;

import io.github.codesapienbe.springvision.core.util.OnnxRuntimeGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

/**
 * Configuration class for ONNX Runtime with support for CPU and GPU execution providers.
 *
 * <p>This configuration dynamically creates ONNX Runtime sessions based on the
 * {@code vision.execution-provider} property. It supports:
 * <ul>
 *   <li>CPU execution (default)</li>
 *   <li>GPU execution with CUDA (requires compatible hardware and drivers)</li>
 * </ul>
 *
 * <p>Example configuration in application.properties:</p>
 * <pre>
 * # For CPU execution (default)
 * vision.execution-provider=cpu
 *
 * # For GPU execution
 * vision.execution-provider=gpu
 * </pre>
 *
 * <p>The GPU profile must be activated at build time:</p>
 * <pre>
 * mvn clean install -P gpu
 * </pre>
 *
 * @author Spring Vision Team
 * @see VisionProperties
 * @see OnnxRuntimeGuard
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(name = "ai.onnxruntime.OrtEnvironment")
@EnableConfigurationProperties(VisionProperties.class)
public class OnnxRuntimeConfig {

    private static final Logger logger = LoggerFactory.getLogger(OnnxRuntimeConfig.class);

    private final VisionProperties visionProperties;

    public OnnxRuntimeConfig(VisionProperties visionProperties) {
        this.visionProperties = visionProperties;
    }

    /**
     * Creates an OrtEnvironment bean for ONNX Runtime operations.
     * The environment is configured based on the execution provider setting.
     *
     * @return the OrtEnvironment instance
     * @throws IllegalStateException if ONNX Runtime is not available
     */
    @Bean
    @ConditionalOnProperty(prefix = "vision", name = "execution-provider")
    public Object ortEnvironment() {
        if (!OnnxRuntimeGuard.isAvailable()) {
            logger.warn("ONNX Runtime is not available on classpath. Add onnxruntime or onnxruntime_gpu dependency.");
            throw new IllegalStateException("ONNX Runtime is not available");
        }

        String provider = visionProperties.getExecutionProvider();
        logger.info("Initializing ONNX Runtime with execution provider: {}", provider);

        try {
            Object environment = OnnxRuntimeGuard.createEnvironment();

            if ("gpu".equalsIgnoreCase(provider)) {
                configureGpuProvider(environment);
            } else {
                logger.info("Using CPU execution provider (default)");
            }

            return environment;
        } catch (Exception e) {
            logger.error("Failed to initialize ONNX Runtime environment", e);
            throw new IllegalStateException("Failed to initialize ONNX Runtime", e);
        }
    }

    /**
     * Creates an OrtSession.SessionOptions bean with the configured execution provider.
     *
     * @return the SessionOptions instance configured with CPU or GPU provider
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.vision", name = "execution-provider")
    public Object ortSessionOptions() {
        if (!OnnxRuntimeGuard.isAvailable()) {
            logger.warn("ONNX Runtime is not available on classpath.");
            return null;
        }

        String provider = visionProperties.getExecutionProvider();

        try {
            // Create SessionOptions using reflection
            Class<?> sessionOptionsClass = Class.forName("ai.onnxruntime.OrtSession$SessionOptions");
            Object sessionOptions = sessionOptionsClass.getDeclaredConstructor().newInstance();

            if ("gpu".equalsIgnoreCase(provider)) {
                try {
                    // Attempt to add CUDA execution provider
                    Method addCudaMethod = sessionOptionsClass.getMethod("addCUDA");
                    addCudaMethod.invoke(sessionOptions);
                    logger.info("Successfully configured CUDA execution provider");
                } catch (NoSuchMethodException e) {
                    logger.warn("CUDA method not found. Ensure you're using onnxruntime_gpu dependency.");
                    fallbackToCpu(sessionOptions);
                } catch (Exception e) {
                    logger.warn("Failed to configure CUDA execution provider. Falling back to CPU. Reason: {}",
                        e.getMessage());
                    fallbackToCpu(sessionOptions);
                }
            } else {
                // CPU is the default, no additional configuration needed
                logger.info("SessionOptions configured for CPU execution");
            }

            return sessionOptions;
        } catch (Exception e) {
            logger.error("Failed to create ONNX Runtime SessionOptions", e);
            throw new IllegalStateException("Failed to create SessionOptions", e);
        }
    }

    /**
     * Configures GPU execution provider for the ONNX Runtime environment.
     * Falls back to CPU if GPU configuration fails.
     *
     * @param environment the OrtEnvironment instance (checked for CUDA availability)
     */
    private void configureGpuProvider(Object environment) {
        try {
            // Check if CUDA is available by attempting to access CUDA-specific classes
            Class.forName("ai.onnxruntime.providers.OrtCUDAProviderOptions");
            logger.info("CUDA provider classes detected. GPU execution will be available.");
            // Environment is configured through SessionOptions, logged for consistency
            logger.debug("Environment checked for GPU support: {}", environment.getClass().getName());
        } catch (ClassNotFoundException e) {
            logger.warn("CUDA provider classes not found. Make sure you're using onnxruntime_gpu dependency " +
                "and have CUDA drivers installed. Falling back to CPU.");
        } catch (Exception e) {
            logger.warn("Error checking for CUDA availability: {}. Falling back to CPU.", e.getMessage());
        }
    }

    /**
     * Fallback mechanism to ensure CPU execution when GPU configuration fails.
     *
     * @param sessionOptions the SessionOptions instance
     */
    private void fallbackToCpu(Object sessionOptions) {
        try {
            Class<?> sessionOptionsClass = Class.forName("ai.onnxruntime.OrtSession$SessionOptions");
            Method addCpuMethod = sessionOptionsClass.getMethod("addCPU", boolean.class);
            addCpuMethod.invoke(sessionOptions, true);
            logger.info("Fallback to CPU execution provider completed");
        } catch (Exception ex) {
            logger.debug("CPU fallback configuration not needed or failed (CPU is default): {}", ex.getMessage());
        }
    }

    /**
     * Helper method to create an OrtSession with the configured options.
     * This is a utility method that can be used by other components.
     *
     * @param modelPath path to the ONNX model file
     * @return the created OrtSession instance
     * @throws Exception if session creation fails
     */
    public Object createSession(String modelPath) throws Exception {
        if (!OnnxRuntimeGuard.isAvailable()) {
            throw new IllegalStateException("ONNX Runtime is not available");
        }

        Object environment = ortEnvironment();
        Object sessionOptions = ortSessionOptions();

        if (sessionOptions != null) {
            // Use reflection to call createSession with options
            Class<?> envClass = Class.forName("ai.onnxruntime.OrtEnvironment");
            Class<?> optionsClass = Class.forName("ai.onnxruntime.OrtSession$SessionOptions");
            Method createSessionMethod = envClass.getMethod("createSession", String.class, optionsClass);
            return createSessionMethod.invoke(environment, modelPath, sessionOptions);
        } else {
            // Fallback to simple session creation
            return OnnxRuntimeGuard.createSession(environment, modelPath);
        }
    }
}
