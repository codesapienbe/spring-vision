package io.github.codesapienbe.springvision.mcp.config;

import io.github.codesapienbe.springvision.core.VectorService;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.backend.OpenCvVisionBackend;
import io.github.codesapienbe.springvision.core.config.OpenCvProperties;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for MCP module to expose a VisionTemplate bean.
 *
 * <p>By default, uses OpenCV backend for local development/testing.
 * For production use with face embeddings, consider configuring one of:
 * <ul>
 *   <li>InsightFace backend - Best for face embeddings and recognition</li>
 *   <li>DeepFace backend - Alternative face recognition</li>
 *   <li>CompreFace backend - Service-based face recognition</li>
 *   <li>FaceBytes backend - Specialized face embeddings</li>
 * </ul>
 *
 * <p>To use a specific backend, define your own VisionTemplate bean
 * in your application configuration with the desired backend.</p>
 */
@Configuration
public class VisionTemplateConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(VisionTemplateConfiguration.class);

    @Value("${spring.vision.backend:opencv}")
    private String backendType;

    /**
     * Default constructor for {@link VisionTemplateConfiguration}.
     */
    public VisionTemplateConfiguration() {
        // Default constructor
    }

    /**
     * Creates a {@link VisionTemplate} bean if one is not already present.
     *
     * @return The configured VisionTemplate.
     */
    @Bean
    @ConditionalOnMissingBean
    public VisionTemplate visionTemplate(VectorService vectorService) {
        logger.info("Initializing VisionTemplate with backend: {}", backendType);

        try {
            // Create an OpenCV backend with default properties
            OpenCvProperties properties = new OpenCvProperties();
            OpenCvVisionBackend backend = new OpenCvVisionBackend(properties);

            // IMPORTANT: Initialize the backend so it's in a healthy state
            backend.initialize();

            // Create VisionTemplate with initialized backend and provided VectorService
            VisionTemplate template = new VisionTemplate(backend, vectorService);

            logger.info("VisionTemplate initialized successfully with OpenCV backend");
            logger.warn("Using default OpenCV backend. For production face embeddings, " +
                "consider using InsightFace, DeepFace, or FaceBytes backends for better quality.");

            return template;
        } catch (BaseVisionException e) {
            logger.error("Failed to initialize OpenCV backend", e);
            throw new RuntimeException("Failed to initialize VisionTemplate", e);
        }
    }
}
