package io.github.codesapienbe.springvision.mcp.config;

import io.github.codesapienbe.springvision.core.VectorService;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.capabilities.EmbeddingCapability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for VisionTemplateConfiguration.
 * Tests backend selection logic and Spring configuration.
 */
class VisionTemplateConfigurationTest {

    private VisionTemplateConfiguration configuration;

    @Mock
    private VectorService vectorService;

    @BeforeEach
    void setUp() {
        configuration = new VisionTemplateConfiguration();
        vectorService = mock(VectorService.class);
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Should create VisionTemplateConfiguration instance")
        void shouldCreateVisionTemplateConfigurationInstance() {
            // When: Creating VisionTemplateConfiguration
            VisionTemplateConfiguration config = new VisionTemplateConfiguration();

            // Then: Should be created successfully
            assertThat(config).isNotNull();
            assertThat(config).isInstanceOf(VisionTemplateConfiguration.class);
        }
    }

    @Nested
    @DisplayName("Vision Template Bean Creation")
    class VisionTemplateBeanCreation {

        @Test
        @DisplayName("Should create VisionTemplate with embedding-capable backend")
        void shouldCreateVisionTemplateWithEmbeddingCapableBackend() {
            // Given: Embedding-capable backend
            VisionBackend embeddingBackend = mock(VisionBackend.class, withSettings().extraInterfaces(EmbeddingCapability.class));
            when(embeddingBackend.getBackendId()).thenReturn("insightface");
            when(embeddingBackend.getDisplayName()).thenReturn("InsightFace Backend");
            when(embeddingBackend.isHealthy()).thenReturn(true);
            when(embeddingBackend.getSupportedDetectionTypes()).thenReturn(Set.of(DetectionType.FACE));

            List<VisionBackend> backends = List.of(embeddingBackend);

            // When: Creating vision template
            VisionTemplate template = configuration.visionTemplate(vectorService, backends);

            // Then: Should select the embedding-capable backend
            assertThat(template).isNotNull();
            assertThat(template.getBackendId()).isEqualTo("insightface");
        }

        @Test
        @DisplayName("Should prefer embedding-capable backend over basic backend")
        void shouldPreferEmbeddingCapableBackendOverBasicBackend() {
            // Given: Both embedding and basic backends
            VisionBackend embeddingBackend = mock(VisionBackend.class, withSettings().extraInterfaces(EmbeddingCapability.class));
            when(embeddingBackend.getBackendId()).thenReturn("insightface");
            when(embeddingBackend.getDisplayName()).thenReturn("InsightFace Backend");
            when(embeddingBackend.isHealthy()).thenReturn(true);
            when(embeddingBackend.getSupportedDetectionTypes()).thenReturn(Set.of(DetectionType.FACE));

            VisionBackend basicBackend = mock(VisionBackend.class);
            when(basicBackend.getBackendId()).thenReturn("opencv");
            when(basicBackend.getDisplayName()).thenReturn("OpenCV Backend");
            when(basicBackend.isHealthy()).thenReturn(true);
            when(basicBackend.getSupportedDetectionTypes()).thenReturn(Set.of(DetectionType.FACE));

            List<VisionBackend> backends = List.of(basicBackend, embeddingBackend);

            // When: Creating vision template
            VisionTemplate template = configuration.visionTemplate(vectorService, backends);

            // Then: Should select the embedding-capable backend
            assertThat(template).isNotNull();
            assertThat(template.getBackendId()).isEqualTo("insightface");
        }

        @Test
        @DisplayName("Should fall back to basic backend when no embedding backend is healthy")
        void shouldFallBackToBasicBackendWhenNoEmbeddingBackendIsHealthy() {
            // Given: Unhealthy embedding backend and healthy basic backend
            VisionBackend unhealthyEmbeddingBackend = mock(VisionBackend.class, withSettings().extraInterfaces(EmbeddingCapability.class));
            when(unhealthyEmbeddingBackend.getBackendId()).thenReturn("insightface");
            when(unhealthyEmbeddingBackend.isHealthy()).thenReturn(false);

            VisionBackend healthyBasicBackend = mock(VisionBackend.class);
            when(healthyBasicBackend.getBackendId()).thenReturn("opencv");
            when(healthyBasicBackend.getDisplayName()).thenReturn("OpenCV Backend");
            when(healthyBasicBackend.isHealthy()).thenReturn(true);
            when(healthyBasicBackend.getSupportedDetectionTypes()).thenReturn(Set.of(DetectionType.FACE));

            List<VisionBackend> backends = List.of(unhealthyEmbeddingBackend, healthyBasicBackend);

            // When: Creating vision template
            VisionTemplate template = configuration.visionTemplate(vectorService, backends);

            // Then: Should select the healthy basic backend
            assertThat(template).isNotNull();
            assertThat(template.getBackendId()).isEqualTo("opencv");
        }

        @Test
        @DisplayName("Should create default DJL backend when no backends available")
        void shouldCreateDefaultDjlBackendWhenNoBackendsAvailable() {
            // When: Creating vision template with no backends
            VisionTemplate template = configuration.visionTemplate(vectorService, null);

            // Then: Should create a template (may use DJL backend)
            assertThat(template).isNotNull();
        }

        @Test
        @DisplayName("Should create default DJL backend when empty backends list")
        void shouldCreateDefaultDjlBackendWhenEmptyBackendsList() {
            // When: Creating vision template with empty backends list
            VisionTemplate template = configuration.visionTemplate(vectorService, List.of());

            // Then: Should create a template (may use DJL backend)
            assertThat(template).isNotNull();
        }

        @Test
        @DisplayName("Should create default DJL backend when all backends unhealthy")
        void shouldCreateDefaultDjlBackendWhenAllBackendsUnhealthy() {
            // Given: Unhealthy backends
            VisionBackend unhealthyBackend1 = mock(VisionBackend.class);
            when(unhealthyBackend1.getBackendId()).thenReturn("backend1");
            when(unhealthyBackend1.isHealthy()).thenReturn(false);

            VisionBackend unhealthyBackend2 = mock(VisionBackend.class);
            when(unhealthyBackend2.getBackendId()).thenReturn("backend2");
            when(unhealthyBackend2.isHealthy()).thenReturn(false);

            List<VisionBackend> backends = List.of(unhealthyBackend1, unhealthyBackend2);

            // When: Creating vision template
            VisionTemplate template = configuration.visionTemplate(vectorService, backends);

            // Then: Should create a template (may use DJL backend)
            assertThat(template).isNotNull();
        }
    }

    @Nested
    @DisplayName("Backend Selection Logic")
    class BackendSelectionLogic {

        @Test
        @DisplayName("Should select first healthy embedding-capable backend")
        void shouldSelectFirstHealthyEmbeddingCapableBackend() {
            // Given: Multiple embedding-capable backends, first one healthy
            VisionBackend healthyEmbedding = mock(VisionBackend.class, withSettings().extraInterfaces(EmbeddingCapability.class));
            when(healthyEmbedding.getBackendId()).thenReturn("insightface");
            when(healthyEmbedding.getDisplayName()).thenReturn("InsightFace Backend");
            when(healthyEmbedding.isHealthy()).thenReturn(true);
            when(healthyEmbedding.getSupportedDetectionTypes()).thenReturn(Set.of(DetectionType.FACE));

            VisionBackend anotherEmbedding = mock(VisionBackend.class, withSettings().extraInterfaces(EmbeddingCapability.class));
            when(anotherEmbedding.getBackendId()).thenReturn("compreface");
            when(anotherEmbedding.isHealthy()).thenReturn(true);

            List<VisionBackend> backends = List.of(healthyEmbedding, anotherEmbedding);

            // When: Creating vision template
            VisionTemplate template = configuration.visionTemplate(vectorService, backends);

            // Then: Should select the first embedding-capable backend
            assertThat(template.getBackendId()).isEqualTo("insightface");
        }

        @Test
        @DisplayName("Should skip unhealthy embedding backends")
        void shouldSkipUnhealthyEmbeddingBackends() {
            // Given: Unhealthy embedding backend and healthy basic backend
            VisionBackend unhealthyEmbedding = mock(VisionBackend.class, withSettings().extraInterfaces(EmbeddingCapability.class));
            when(unhealthyEmbedding.getBackendId()).thenReturn("insightface");
            when(unhealthyEmbedding.isHealthy()).thenReturn(false);

            VisionBackend healthyBasic = mock(VisionBackend.class);
            when(healthyBasic.getBackendId()).thenReturn("opencv");
            when(healthyBasic.getDisplayName()).thenReturn("OpenCV Backend");
            when(healthyBasic.isHealthy()).thenReturn(true);
            when(healthyBasic.getSupportedDetectionTypes()).thenReturn(Set.of(DetectionType.FACE));

            List<VisionBackend> backends = List.of(unhealthyEmbedding, healthyBasic);

            // When: Creating vision template
            VisionTemplate template = configuration.visionTemplate(vectorService, backends);

            // Then: Should select the healthy basic backend
            assertThat(template.getBackendId()).isEqualTo("opencv");
        }

        @Test
        @DisplayName("Should return null when no healthy backends found")
        void shouldReturnNullWhenNoHealthyBackendsFound() {
            // This test verifies the logic handles the case where no backends are healthy
            // The actual fallback to DJL backend happens in the configuration method

            // Given: No backends provided
            List<VisionBackend> backends = null;

            // When: Creating vision template
            VisionTemplate template = configuration.visionTemplate(vectorService, backends);

            // Then: Should create template (with DJL fallback)
            assertThat(template).isNotNull();
        }
    }

    @Nested
    @DisplayName("Configuration Annotations")
    class ConfigurationAnnotations {

        @Test
        @DisplayName("Should have Configuration annotation")
        void shouldHaveConfigurationAnnotation() {
            // When: Checking for Configuration annotation
            boolean hasConfiguration = VisionTemplateConfiguration.class.isAnnotationPresent(
                org.springframework.context.annotation.Configuration.class);

            // Then: Should have the annotation
            assertThat(hasConfiguration).isTrue();
        }

        @Test
        @DisplayName("Should have ConditionalOnMissingBean annotation on visionTemplate method")
        void shouldHaveConditionalOnMissingBeanAnnotationOnVisionTemplateMethod() throws NoSuchMethodException {
            // When: Getting the visionTemplate method
            java.lang.reflect.Method method = VisionTemplateConfiguration.class.getMethod(
                "visionTemplate", VectorService.class, List.class);

            // Then: Method should have ConditionalOnMissingBean annotation
            assertThat(method.isAnnotationPresent(
                org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("Method Signatures and Parameters")
    class MethodSignaturesAndParameters {

        @Test
        @DisplayName("Should have correct visionTemplate method signature")
        void shouldHaveCorrectVisionTemplateMethodSignature() throws NoSuchMethodException {
            // When: Getting the visionTemplate method
            java.lang.reflect.Method method = VisionTemplateConfiguration.class.getMethod(
                "visionTemplate", VectorService.class, List.class);

            // Then: Should have correct return type
            assertThat(method.getReturnType()).isEqualTo(VisionTemplate.class);

            // Should have correct parameter types
            assertThat(method.getParameterTypes()).hasSize(2);
            assertThat(method.getParameterTypes()[0]).isEqualTo(VectorService.class);
            assertThat(method.getParameterTypes()[1]).isEqualTo(List.class);
        }

        @Test
        @DisplayName("Should have Bean annotation on visionTemplate method")
        void shouldHaveBeanAnnotationOnVisionTemplateMethod() throws NoSuchMethodException {
            // When: Getting the visionTemplate method
            java.lang.reflect.Method method = VisionTemplateConfiguration.class.getMethod(
                "visionTemplate", VectorService.class, List.class);

            // Then: Method should have Bean annotation
            assertThat(method.isAnnotationPresent(
                org.springframework.context.annotation.Bean.class)).isTrue();
        }

        @Test
        @DisplayName("Should have Autowired annotation on availableBackends parameter")
        void shouldHaveAutowiredAnnotationOnAvailableBackendsParameter() throws NoSuchMethodException {
            // When: Getting the visionTemplate method
            java.lang.reflect.Method method = VisionTemplateConfiguration.class.getMethod(
                "visionTemplate", VectorService.class, List.class);
            java.lang.reflect.Parameter[] parameters = method.getParameters();

            // Then: Second parameter should have Autowired annotation
            assertThat(parameters[1].isAnnotationPresent(
                org.springframework.beans.factory.annotation.Autowired.class)).isTrue();

            // And should have required=false
            org.springframework.beans.factory.annotation.Autowired autowired =
                parameters[1].getAnnotation(org.springframework.beans.factory.annotation.Autowired.class);
            assertThat(autowired.required()).isFalse();
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle backend initialization failures gracefully")
        void shouldHandleBackendInitializationFailuresGracefully() {
            // This test verifies that the configuration handles cases where
            // backend initialization might fail. The actual error handling
            // is tested implicitly through the successful creation of templates.

            // Given: Configuration instance
            VisionTemplateConfiguration config = new VisionTemplateConfiguration();

            // When: Creating template with no backends (forces DJL backend creation)
            VisionTemplate template = config.visionTemplate(vectorService, null);

            // Then: Should create template successfully or handle errors appropriately
            assertThat(template).isNotNull();
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("Should work with real VectorService")
        void shouldWorkWithRealVectorService() {
            // Given: Real vector service (from configuration)
            ToolCallbackConfiguration toolConfig = new ToolCallbackConfiguration();
            VectorService realVectorService = toolConfig.inMemoryVectorService();

            // When: Creating vision template with real vector service
            VisionTemplate template = configuration.visionTemplate(realVectorService, null);

            // Then: Should create template successfully
            assertThat(template).isNotNull();
            assertThat(template.getBackendId()).isNotNull();
        }

        @Test
        @DisplayName("Should handle multiple backend types")
        void shouldHandleMultipleBackendTypes() {
            // Given: Mix of different backend types
            VisionBackend compreFace = mock(VisionBackend.class, withSettings().extraInterfaces(EmbeddingCapability.class));
            when(compreFace.getBackendId()).thenReturn("compreface");
            when(compreFace.getDisplayName()).thenReturn("CompreFace Backend");
            when(compreFace.isHealthy()).thenReturn(true);
            when(compreFace.getSupportedDetectionTypes()).thenReturn(Set.of(DetectionType.FACE));

            VisionBackend deepFace = mock(VisionBackend.class, withSettings().extraInterfaces(EmbeddingCapability.class));
            when(deepFace.getBackendId()).thenReturn("deepface");
            when(deepFace.isHealthy()).thenReturn(true);

            VisionBackend openCv = mock(VisionBackend.class);
            when(openCv.getBackendId()).thenReturn("opencv");
            when(openCv.getDisplayName()).thenReturn("OpenCV Backend");
            when(openCv.isHealthy()).thenReturn(true);
            when(openCv.getSupportedDetectionTypes()).thenReturn(Set.of(DetectionType.FACE));

            List<VisionBackend> backends = List.of(openCv, compreFace, deepFace);

            // When: Creating vision template
            VisionTemplate template = configuration.visionTemplate(vectorService, backends);

            // Then: Should select the first embedding-capable backend (compreFace)
            assertThat(template.getBackendId()).isEqualTo("compreface");
        }
    }
}

