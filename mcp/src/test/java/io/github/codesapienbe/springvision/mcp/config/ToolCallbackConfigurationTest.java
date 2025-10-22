package io.github.codesapienbe.springvision.mcp.config;

import io.github.codesapienbe.springvision.mcp.VisionTool;
import io.github.codesapienbe.springvision.core.VectorService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ToolCallbackConfiguration.
 * Tests Spring configuration and bean creation.
 */
class ToolCallbackConfigurationTest {

    private ToolCallbackConfiguration configuration;

    public ToolCallbackConfigurationTest() {
        configuration = new ToolCallbackConfiguration();
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Should create ToolCallbackConfiguration instance")
        void shouldCreateToolCallbackConfigurationInstance() {
            // When: Creating ToolCallbackConfiguration
            ToolCallbackConfiguration config = new ToolCallbackConfiguration();

            // Then: Should be created successfully
            assertThat(config).isNotNull();
            assertThat(config).isInstanceOf(ToolCallbackConfiguration.class);
        }
    }

    @Nested
    @DisplayName("Tools Bean")
    class ToolsBean {

        @Test
        @DisplayName("Should create ToolCallbackProvider with VisionTool")
        void shouldCreateToolCallbackProviderWithVisionTool() {
            // Given: Mock VisionTool
            VisionTool visionTool = mock(VisionTool.class);

            // When: Creating tools bean with VisionTool
            ToolCallbackProvider provider = configuration.tools(visionTool);

            // Then: Should return a MethodToolCallbackProvider
            assertThat(provider).isNotNull();
            assertThat(provider).isInstanceOf(MethodToolCallbackProvider.class);
        }

        @Test
        @DisplayName("Should create ToolCallbackProvider without VisionTool")
        void shouldCreateToolCallbackProviderWithoutVisionTool() {
            // When: Creating tools bean with null VisionTool
            ToolCallbackProvider provider = configuration.tools(null);

            // Then: Should return a MethodToolCallbackProvider (empty)
            assertThat(provider).isNotNull();
            assertThat(provider).isInstanceOf(MethodToolCallbackProvider.class);
        }

        @Test
        @DisplayName("Should handle null VisionTool gracefully")
        void shouldHandleNullVisionToolGracefully() {
            // When: Creating tools bean with null
            ToolCallbackProvider provider = configuration.tools(null);

            // Then: Should create provider without throwing exception
            assertThat(provider).isNotNull();
        }
    }

    @Nested
    @DisplayName("In Memory Vector Service Bean")
    class InMemoryVectorServiceBean {

        @Test
        @DisplayName("Should create InMemoryVectorService bean")
        void shouldCreateInMemoryVectorServiceBean() {
            // When: Creating inMemoryVectorService bean
            VectorService vectorService = configuration.inMemoryVectorService();

            // Then: Should return an InMemoryVectorService instance
            assertThat(vectorService).isNotNull();
            assertThat(vectorService).isInstanceOf(InMemoryVectorService.class);
            assertThat(vectorService).isInstanceOf(VectorService.class);
        }

        @Test
        @DisplayName("Should return fresh instance each time")
        void shouldReturnFreshInstanceEachTime() {
            // When: Creating multiple instances
            VectorService service1 = configuration.inMemoryVectorService();
            VectorService service2 = configuration.inMemoryVectorService();

            // Then: Should be different instances
            assertThat(service1).isNotSameAs(service2);
            assertThat(service1).isNotEqualTo(service2); // Different objects
        }

        @Test
        @DisplayName("Should implement VectorService interface")
        void shouldImplementVectorServiceInterface() {
            // When: Creating vector service
            VectorService vectorService = configuration.inMemoryVectorService();

            // Then: Should implement VectorService interface
            assertThat(vectorService).isInstanceOf(VectorService.class);

            // Test basic functionality
            String id = vectorService.storeFaceEmbedding(
                "test-person", new float[]{0.1f, 0.2f}, "test-model",
                "test-hash", 0.9, null);

            assertThat(id).isNotNull();
            assertThat(id).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Configuration Annotations")
    class ConfigurationAnnotations {

        @Test
        @DisplayName("Should have Configuration annotation")
        void shouldHaveConfigurationAnnotation() {
            // When: Checking for Configuration annotation
            boolean hasConfiguration = ToolCallbackConfiguration.class.isAnnotationPresent(
                org.springframework.context.annotation.Configuration.class);

            // Then: Should have the annotation
            assertThat(hasConfiguration).isTrue();
        }
    }

    @Nested
    @DisplayName("Bean Methods")
    class BeanMethods {

        @Test
        @DisplayName("Should have tools method with Bean annotation")
        void shouldHaveToolsMethodWithBeanAnnotation() throws NoSuchMethodException {
            // When: Getting the tools method
            java.lang.reflect.Method method = ToolCallbackConfiguration.class.getMethod(
                "tools", VisionTool.class);

            // Then: Method should exist and have Bean annotation
            assertThat(method).isNotNull();
            assertThat(method.isAnnotationPresent(
                org.springframework.context.annotation.Bean.class)).isTrue();
        }

        @Test
        @DisplayName("Should have inMemoryVectorService method with Bean annotation")
        void shouldHaveInMemoryVectorServiceMethodWithBeanAnnotation() throws NoSuchMethodException {
            // When: Getting the inMemoryVectorService method
            java.lang.reflect.Method method = ToolCallbackConfiguration.class.getMethod("inMemoryVectorService");

            // Then: Method should exist and have Bean annotation
            assertThat(method).isNotNull();
            assertThat(method.isAnnotationPresent(
                org.springframework.context.annotation.Bean.class)).isTrue();
        }

        @Test
        @DisplayName("Should have Nullable annotation on VisionTool parameter")
        void shouldHaveNullableAnnotationOnVisionToolParameter() throws NoSuchMethodException {
            // When: Getting the tools method
            java.lang.reflect.Method method = ToolCallbackConfiguration.class.getMethod(
                "tools", VisionTool.class);
            java.lang.reflect.Parameter[] parameters = method.getParameters();

            // Then: First parameter should have Nullable annotation
            assertThat(parameters[0].isAnnotationPresent(
                org.springframework.lang.Nullable.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("Integration with Spring Context")
    class IntegrationWithSpringContext {

        @Test
        @DisplayName("Should be compatible with Spring bean creation")
        void shouldBeCompatibleWithSpringBeanCreation() {
            // This test verifies that the configuration class can be used
            // in a Spring context without issues

            // Given: Configuration instance
            ToolCallbackConfiguration config = new ToolCallbackConfiguration();

            // When: Creating beans
            VectorService vectorService = config.inMemoryVectorService();
            ToolCallbackProvider toolProvider = config.tools(mock(VisionTool.class));

            // Then: Should create valid beans
            assertThat(vectorService).isNotNull();
            assertThat(toolProvider).isNotNull();
        }

        @Test
        @DisplayName("Should handle VisionTool lifecycle correctly")
        void shouldHandleVisionToolLifecycleCorrectly() {
            // Given: Mock VisionTool
            VisionTool visionTool = mock(VisionTool.class);

            // When: Creating tool provider with VisionTool
            ToolCallbackProvider provider = configuration.tools(visionTool);

            // Then: Provider should be created successfully
            assertThat(provider).isNotNull();
            // Note: The actual tool registration would happen in Spring AI's infrastructure
        }
    }

    @Nested
    @DisplayName("Method Signatures")
    class MethodSignatures {

        @Test
        @DisplayName("Should have correct tools method signature")
        void shouldHaveCorrectToolsMethodSignature() throws NoSuchMethodException {
            // When: Getting the tools method
            java.lang.reflect.Method method = ToolCallbackConfiguration.class.getMethod(
                "tools", VisionTool.class);

            // Then: Should have correct return type and parameters
            assertThat(method.getReturnType()).isEqualTo(ToolCallbackProvider.class);
            assertThat(method.getParameterTypes()).hasSize(1);
            assertThat(method.getParameterTypes()[0]).isEqualTo(VisionTool.class);
        }

        @Test
        @DisplayName("Should have correct inMemoryVectorService method signature")
        void shouldHaveCorrectInMemoryVectorServiceMethodSignature() throws NoSuchMethodException {
            // When: Getting the inMemoryVectorService method
            java.lang.reflect.Method method = ToolCallbackConfiguration.class.getMethod("inMemoryVectorService");

            // Then: Should have correct return type and no parameters
            assertThat(method.getReturnType()).isEqualTo(VectorService.class);
            assertThat(method.getParameterTypes()).hasSize(0);
        }
    }
}
