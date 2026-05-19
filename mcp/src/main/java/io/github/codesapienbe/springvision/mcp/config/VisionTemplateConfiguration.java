package io.github.codesapienbe.springvision.mcp.config;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.codesapienbe.springvision.core.VectorService;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.capabilities.EmbeddingCapability;
import io.github.codesapienbe.springvision.core.djl.DjlOnlineDamageClassifier;
import io.github.codesapienbe.springvision.core.djl.DjlProperties;
import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;

/**
 * Configuration for MCP module to expose a VisionTemplate bean.
 *
 * <p>This configuration intelligently selects the best available backend:
 * <ul>
 *   <li>InsightFace backend - Best for face embeddings and recognition</li>
 *   <li>DeepFace backend - Alternative face recognition</li>
 *   <li>CompreFace backend - Service-based face recognition</li>
 *   <li>OpenCV backend - Fallback for face detection only</li>
 * </ul>
 *
 * <p>To use a specific backend, define your own VisionTemplate bean
 * in your application configuration with the desired backend.</p>
 */
@Configuration
@EnableConfigurationProperties(DjlProperties.class)
public class VisionTemplateConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(VisionTemplateConfiguration.class);

    /**
     * Default constructor for {@link VisionTemplateConfiguration}.
     */
    public VisionTemplateConfiguration() {
        // Default constructor
    }

    /**
     * Creates a {@link VisionTemplate} bean if one is not already present.
     *
     * <p>This method intelligently selects the best available backend based on capabilities:
     * <ol>
     *   <li>Prefers embedding-capable backends (InsightFace, CompreFace, etc.) for full MCP tool support</li>
     *   <li>Falls back to OpenCV for basic face detection if no embedding backend is available</li>
     * </ol>
     *
     * @param vectorService     optional vector service for similarity operations
     * @param availableBackends all registered VisionBackend beans
     * @return The configured VisionTemplate with the best available backend
     */
    @Bean
    @ConditionalOnMissingBean
    public VisionTemplate visionTemplate(
        VectorService vectorService,
        @Autowired(required = false) List<VisionBackend> availableBackends,
        @Autowired(required = false) DjlProperties djlProperties) {

        logger.info("Initializing VisionTemplate - scanning for available backends...");

        // Try to find an embedding-capable backend first (preferred for MCP tools)
        VisionBackend selectedBackend = null;

        if (availableBackends != null && !availableBackends.isEmpty()) {
            logger.info("Found {} registered backend(s)", availableBackends.size());

            // Log all available backends
            for (VisionBackend backend : availableBackends) {
                boolean supportsEmbeddings = backend instanceof EmbeddingCapability;
                logger.info("  - {}: {} (embeddings: {})",
                    backend.getBackendId(),
                    backend.getDisplayName(),
                    supportsEmbeddings);
            }

            // Prefer embedding-capable backends for MCP tools
            Optional<VisionBackend> embeddingBackend = availableBackends.stream()
                .filter(b -> b instanceof EmbeddingCapability)
                .filter(VisionBackend::isHealthy)
                .findFirst();

            if (embeddingBackend.isPresent()) {
                selectedBackend = embeddingBackend.get();
                logger.info("Selected embedding-capable backend: {} (supports full MCP tool functionality)",
                    selectedBackend.getBackendId());
            } else {
                // Fall back to any healthy backend
                Optional<VisionBackend> anyHealthyBackend = availableBackends.stream()
                    .filter(VisionBackend::isHealthy)
                    .findFirst();

                if (anyHealthyBackend.isPresent()) {
                    selectedBackend = anyHealthyBackend.get();
                    logger.warn("Selected backend '{}' does NOT support embeddings - " +
                            "embedding extraction will fail. Consider enabling InsightFace or CompreFace.",
                        selectedBackend.getBackendId());
                }
            }
        }

        // Build online damage classifier if enabled in config
        DjlOnlineDamageClassifier onlineClassifier = null;
        DjlProperties effectiveProps = djlProperties != null ? djlProperties : new DjlProperties();
        if (effectiveProps.getOnlineClassifier().isEnabled()) {
            try {
                onlineClassifier = new DjlOnlineDamageClassifier(effectiveProps);
                onlineClassifier.initialize();
                logger.info("Online damage classifier initialized (dataset dir: {})",
                    effectiveProps.getOnlineClassifier().getDatasetDir());
            } catch (Exception e) {
                logger.warn("Failed to initialize online damage classifier — label submission disabled: {}",
                    e.getMessage());
            }
        }

        // If no backend found or selected, create the default DJL backend
        if (selectedBackend == null) {
            logger.warn("No healthy backends found - initializing default DJL backend");
            logger.info("DJL backend supports face detection and modern AI models");

            try {
                DjlVisionBackend djlBackend = new DjlVisionBackend();
                if (onlineClassifier != null) {
                    djlBackend.setOnlineClassifier(onlineClassifier);
                    logger.info("Online damage classifier attached to DJL backend");
                }
                djlBackend.initialize();
                selectedBackend = djlBackend;
                logger.info("Default DJL backend initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize default DJL backend", e);
                throw new RuntimeException("Failed to initialize VisionTemplate - no backends available", e);
            }
        } else if (selectedBackend instanceof DjlVisionBackend djlBackend && onlineClassifier != null) {
            djlBackend.setOnlineClassifier(onlineClassifier);
            logger.info("Online damage classifier attached to existing DJL backend");
        }

        // Create VisionTemplate with the selected backend
        VisionTemplate template = new VisionTemplate(selectedBackend, vectorService);

        logger.info("=== VisionTemplate Configuration Summary ===");
        logger.info("Backend: {} ({})", selectedBackend.getBackendId(), selectedBackend.getDisplayName());
        logger.info("Embeddings Support: {}", selectedBackend instanceof EmbeddingCapability ? "YES" : "NO");
        logger.info("Supported Detection Types: {}", selectedBackend.getSupportedDetectionTypes());
        logger.info("============================================");

        return template;
    }
}
