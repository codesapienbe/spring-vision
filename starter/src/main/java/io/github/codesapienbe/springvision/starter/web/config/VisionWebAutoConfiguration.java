package io.github.codesapienbe.springvision.starter.web.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;
import io.github.codesapienbe.springvision.starter.web.VisionController;

/**
 * Registers the {@code /api/vision/*} endpoints in a servlet web application, only when
 * {@code spring.vision.web.enabled=true}.
 */
@AutoConfiguration(after = VisionAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(VisionTemplate.class)
@ConditionalOnProperty(prefix = "spring.vision.web", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(VisionWebProperties.class)
@Import({VisionController.class, AsyncConfig.class})
public class VisionWebAutoConfiguration {
}
