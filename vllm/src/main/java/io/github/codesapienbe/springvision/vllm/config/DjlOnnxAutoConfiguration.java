package io.github.codesapienbe.springvision.vllm.config;

import io.github.codesapienbe.springvision.vllm.onnx.OptimizedDjlVisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for DJL ONNX Runtime backend.
 *
 * @author Spring Vision Team
 * @since 1.0.3
 */
@AutoConfiguration
@ConditionalOnClass({OptimizedDjlVisionBackend.class, ai.djl.Model.class})
@ConditionalOnProperty(prefix = "spring.vision.vllm.embedded.djl", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DjlOnnxProperties.class)
public class DjlOnnxAutoConfiguration {

    /**
     * Creates OptimizedDjlVisionBackend bean if not already defined.
     *
     * @param properties DJL ONNX configuration properties
     * @return configured OptimizedDjlVisionBackend instance
     */
    @Bean
    @ConditionalOnMissingBean
    public OptimizedDjlVisionBackend optimizedDjlVisionBackend(DjlOnnxProperties properties) {
        return new OptimizedDjlVisionBackend(
            properties.modelPath(),
            properties.executionProvider(),
            properties.confidenceThreshold(),
            properties.inputSize()
        );
    }

    /**
     * Creates VisionTemplate bean configured with OptimizedDjlVisionBackend.
     *
     * @param backend the DJL vision backend
     * @return configured VisionTemplate instance
     */
    @Bean
    @ConditionalOnMissingBean(name = "djlOnnxVisionTemplate")
    public VisionTemplate djlOnnxVisionTemplate(OptimizedDjlVisionBackend backend) {
        return new VisionTemplate(backend);
    }
}
