package io.github.codesapienbe.springvision.starter.web.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;
import io.github.codesapienbe.springvision.starter.web.VisionController;

/**
 * What a downstream application gets from spring-vision's auto-configuration. A mock backend is
 * supplied so no model is loaded.
 */
@DisplayName("spring-vision auto-configuration in a downstream application")
class VisionWebAutoConfigurationTest {

    private static VisionBackend mockBackend() {
        VisionBackend backend = mock(VisionBackend.class);
        when(backend.getBackendId()).thenReturn("mock");
        return backend;
    }

    private final WebApplicationContextRunner web = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(VisionAutoConfiguration.class, VisionWebAutoConfiguration.class))
        .withPropertyValues("vision.health.enabled=false", "vision.metrics.enabled=false")
        .withBean(VisionBackend.class, VisionWebAutoConfigurationTest::mockBackend);

    @Test
    @DisplayName("registers both auto-configurations in AutoConfiguration.imports")
    void registersAutoConfigurations() {
        assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates())
            .contains(VisionAutoConfiguration.class.getName(), VisionWebAutoConfiguration.class.getName());
    }

    @Test
    @DisplayName("gives exactly one VisionTemplate and exposes no endpoints by default")
    void oneTemplateNoEndpointsByDefault() {
        web.run(context -> {
            assertThat(context).hasSingleBean(VisionTemplate.class);
            assertThat(context).doesNotHaveBean(VisionController.class);
        });
    }

    @Test
    @DisplayName("registers the endpoints when spring.vision.web.enabled=true")
    void endpointsWhenEnabled() {
        web.withPropertyValues("spring.vision.web.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(VisionController.class);
            assertThat(context.getBean(VisionWebProperties.class).remoteImageUrls()).isFalse();
        });
    }

    @Test
    @DisplayName("registers no endpoints outside a web application, even when enabled")
    void noEndpointsOutsideWebApplication() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VisionAutoConfiguration.class, VisionWebAutoConfiguration.class))
            .withPropertyValues("vision.health.enabled=false", "vision.metrics.enabled=false", "spring.vision.web.enabled=true")
            .withBean(VisionBackend.class, VisionWebAutoConfigurationTest::mockBackend)
            .run(context -> {
                assertThat(context).hasSingleBean(VisionTemplate.class);
                assertThat(context).doesNotHaveBean(VisionController.class);
            });
    }
}
