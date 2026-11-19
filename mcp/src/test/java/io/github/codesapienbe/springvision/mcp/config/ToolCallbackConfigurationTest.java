package io.github.codesapienbe.springvision.mcp.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;

import io.github.codesapienbe.springvision.core.VectorService;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.mcp.SetupTool;
import io.github.codesapienbe.springvision.mcp.VisionTool;

/**
 * Comprehensive unit tests for ToolCallbackConfiguration.
 * Tests Spring configuration and bean creation.
 */
class ToolCallbackConfigurationTest {

    private static final McpSecurityProperties ADMIN_ONLY = new McpSecurityProperties(null,
            List.of(new McpSecurityProperties.ToolGrant("mcp-admin", List.of(ToolAccessPolicy.ALL_TOOLS))), null);

    private ToolCallbackConfiguration configuration;

    private static VisionTool visionTool() {
        return new VisionTool(new VisionTemplate(mock(VisionBackend.class)));
    }

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
            // Given: a VisionTool
            VisionTool visionTool = visionTool();

            // When: Creating tools bean with VisionTool
            ToolCallbackProvider provider = configuration.tools(visionTool, ADMIN_ONLY, false);

            // Then: Every tool is registered, each behind the access guard
            assertThat(provider.getToolCallbacks()).isNotEmpty()
                    .allSatisfy(callback -> assertThat(callback).isInstanceOf(GuardedToolCallback.class));
        }

        @Test
        @DisplayName("Should create ToolCallbackProvider without VisionTool")
        void shouldCreateToolCallbackProviderWithoutVisionTool() {
            // When: Creating tools bean with null VisionTool
            ToolCallbackProvider provider = configuration.tools(null, ADMIN_ONLY, false);

            // Then: Should return an empty provider
            assertThat(provider.getToolCallbacks()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null VisionTool gracefully")
        void shouldHandleNullVisionToolGracefully() {
            // When: Creating tools bean with null
            ToolCallbackProvider provider = configuration.tools(null, ADMIN_ONLY, false);

            // Then: Should create provider without throwing exception
            assertThat(provider).isNotNull();
        }
    }

    @Nested
    @DisplayName("Tool access")
    class ToolAccess {

        @Test
        @DisplayName("Fails start-up when a grant names a tool that does not exist")
        void failsOnUnknownToolName() {
            McpSecurityProperties typo = new McpSecurityProperties(null,
                    List.of(new McpSecurityProperties.ToolGrant("mcp-user", List.of("count_face_b"))), null);

            assertThatThrownBy(() -> configuration.tools(visionTool(), typo, false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("count_face_b");
        }

        @Test
        @DisplayName("Over stdio the caller has the configured stdio roles, without a token")
        void stdioCallerHasStdioRoles() {
            McpSecurityProperties stdioUser = new McpSecurityProperties(null,
                    List.of(new McpSecurityProperties.ToolGrant("local", List.of("list_identities"))), List.of("local"));
            ToolCallbackProvider provider = configuration.tools(visionTool(), stdioUser, true);

            // The mock backend has no identity store, so getting that far proves the guard let it through.
            assertThatThrownBy(() -> callback(provider, "list_identities").call("{}"))
                    .isNotInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageContaining("enrolled-user store");
            assertThatThrownBy(() -> callback(provider, "count_faces_b").call("{}"))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }

        private org.springframework.ai.tool.ToolCallback callback(ToolCallbackProvider provider, String name) {
            return java.util.Arrays.stream(provider.getToolCallbacks())
                    .filter(callback -> callback.getToolDefinition().name().equals(name))
                    .findFirst()
                    .orElseThrow();
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
                "tools", VisionTool.class, SetupTool.class, McpSecurityProperties.class, boolean.class,
                ObjectProvider.class, Duration.class);

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
                "tools", VisionTool.class, SetupTool.class, McpSecurityProperties.class, boolean.class,
                ObjectProvider.class, Duration.class);
            java.lang.reflect.Parameter[] parameters = method.getParameters();

            // Then: The VisionTool and SetupTool parameters should have the Nullable annotation
            assertThat(parameters[0].isAnnotationPresent(
                org.springframework.lang.Nullable.class)).isTrue();
            assertThat(parameters[1].isAnnotationPresent(
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
            ToolCallbackProvider toolProvider = config.tools(visionTool(), ADMIN_ONLY, false);

            // Then: Should create valid beans
            assertThat(vectorService).isNotNull();
            assertThat(toolProvider).isNotNull();
        }

        @Test
        @DisplayName("Should handle VisionTool lifecycle correctly")
        void shouldHandleVisionToolLifecycleCorrectly() {
            // Given: a VisionTool
            VisionTool visionTool = visionTool();

            // When: Creating tool provider with VisionTool
            ToolCallbackProvider provider = configuration.tools(visionTool, ADMIN_ONLY, false);

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
                "tools", VisionTool.class, SetupTool.class, McpSecurityProperties.class, boolean.class,
                ObjectProvider.class, Duration.class);

            // Then: Should have correct return type and parameters
            assertThat(method.getReturnType()).isEqualTo(ToolCallbackProvider.class);
            assertThat(method.getParameterTypes()).hasSize(6);
            assertThat(method.getParameterTypes()[0]).isEqualTo(VisionTool.class);
            assertThat(method.getParameterTypes()[1]).isEqualTo(SetupTool.class);
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
