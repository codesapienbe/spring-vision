package io.github.codesapienbe.springvision.tesseract.config;

import io.github.codesapienbe.springvision.tesseract.TesseractVisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Spring Vision Tesseract module.
 *
 * <p>This configuration is activated when:
 * <ul>
 *   <li>spring.vision.tesseract.enabled=true</li>
 *   <li>TesseractVisionBackend class is on the classpath</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(TesseractVisionBackend.class)
@ConditionalOnProperty(prefix = "spring.vision.tesseract", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(TesseractProperties.class)
public class TesseractAutoConfiguration {

    /**
     * Creates TesseractVisionBackend bean if not already defined.
     *
     * @param properties the Tesseract configuration properties
     * @return configured TesseractVisionBackend instance
     */
    @Bean
    @ConditionalOnMissingBean
    public TesseractVisionBackend tesseractVisionBackend(TesseractProperties properties) {
        return new TesseractVisionBackend();
    }

    /**
     * Creates VisionTemplate bean configured with TesseractVisionBackend if not already defined.
     *
     * @param backend the Tesseract backend
     * @return configured VisionTemplate instance
     */
    @Bean
    @ConditionalOnMissingBean(name = "tesseractVisionTemplate")
    public VisionTemplate tesseractVisionTemplate(TesseractVisionBackend backend) {
        return new VisionTemplate(backend);
    }
}

