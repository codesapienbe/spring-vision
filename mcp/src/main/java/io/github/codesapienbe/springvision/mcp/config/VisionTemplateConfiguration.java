package io.github.codesapienbe.springvision.mcp.config;

import io.github.codesapienbe.springvision.core.VisionTemplate;
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
     * @return The configured VisionTemplate.
     */
    @Bean
    @ConditionalOnMissingBean
    public VisionTemplate visionTemplate() {
        logger.info("Initializing VisionTemplate with backend: {}", backendType);

        // For now, use the default OpenCV backend
        // Users can override this bean to use a different backend
        VisionTemplate template = new VisionTemplate();

        logger.warn("Using default OpenCV backend. For production face embeddings, " +
            "consider using InsightFace, DeepFace, or FaceBytes backends for better quality.");

        return template;
    }
}
