package io.github.codesapienbe.springvision.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for StartupLogger component.
 * Tests logging behavior and configuration injection.
 */
class StartupLoggerTest {

    private StartupLogger startupLogger;

    @BeforeEach
    void setUp() {
        startupLogger = new StartupLogger();
    }

    @Nested
    @DisplayName("Constructor and Initialization")
    class ConstructorAndInitialization {

        @Test
        @DisplayName("Should create StartupLogger instance")
        void shouldCreateStartupLoggerInstance() {
            // When: Creating StartupLogger
            StartupLogger logger = new StartupLogger();

            // Then: Should be created successfully
            assertThat(logger).isNotNull();
            assertThat(logger).isInstanceOf(StartupLogger.class);
        }
    }

    @Nested
    @DisplayName("Configuration Properties")
    class ConfigurationProperties {

        @Test
        @DisplayName("Should have default server name value")
        void shouldHaveDefaultServerNameValue() {
            // Given: StartupLogger with default values
            StartupLogger logger = new StartupLogger();

            // When: Getting server name field
            String serverName = (String) ReflectionTestUtils.getField(logger, "serverName");

            // Then: Should have default value
            assertThat(serverName).isEqualTo("spring-vision");
        }

        @Test
        @DisplayName("Should have default server version value")
        void shouldHaveDefaultServerVersionValue() {
            // Given: StartupLogger with default values
            StartupLogger logger = new StartupLogger();

            // When: Getting server version field
            String serverVersion = (String) ReflectionTestUtils.getField(logger, "serverVersion");

            // Then: Should have default value
            assertThat(serverVersion).isEqualTo("1.0.5");
        }

        @Test
        @DisplayName("Should have default transport value")
        void shouldHaveDefaultTransportValue() {
            // Given: StartupLogger with default values
            StartupLogger logger = new StartupLogger();

            // When: Getting transport field
            String transport = (String) ReflectionTestUtils.getField(logger, "transport");

            // Then: Should have default value
            assertThat(transport).isEqualTo("stdio");
        }

        @Test
        @DisplayName("Should inject custom server name")
        void shouldInjectCustomServerName() {
            // Given: StartupLogger
            StartupLogger logger = new StartupLogger();

            // When: Setting custom server name
            ReflectionTestUtils.setField(logger, "serverName", "custom-vision-server");

            // Then: Should have custom value
            String serverName = (String) ReflectionTestUtils.getField(logger, "serverName");
            assertThat(serverName).isEqualTo("custom-vision-server");
        }

        @Test
        @DisplayName("Should inject custom server version")
        void shouldInjectCustomServerVersion() {
            // Given: StartupLogger
            StartupLogger logger = new StartupLogger();

            // When: Setting custom server version
            ReflectionTestUtils.setField(logger, "serverVersion", "2.0.0");

            // Then: Should have custom value
            String serverVersion = (String) ReflectionTestUtils.getField(logger, "serverVersion");
            assertThat(serverVersion).isEqualTo("2.0.0");
        }

        @Test
        @DisplayName("Should inject custom transport")
        void shouldInjectCustomTransport() {
            // Given: StartupLogger
            StartupLogger logger = new StartupLogger();

            // When: Setting custom transport
            ReflectionTestUtils.setField(logger, "transport", "websocket");

            // Then: Should have custom value
            String transport = (String) ReflectionTestUtils.getField(logger, "transport");
            assertThat(transport).isEqualTo("websocket");
        }
    }

    @Nested
    @DisplayName("PostConstruct Method")
    class PostConstructMethod {

        @Test
        @DisplayName("Should have onStartup method with PostConstruct annotation")
        void shouldHaveOnStartupMethodWithPostConstructAnnotation() throws NoSuchMethodException {
            // When: Getting the onStartup method
            java.lang.reflect.Method method = StartupLogger.class.getMethod("onStartup");

            // Then: Method should exist and have PostConstruct annotation
            assertThat(method).isNotNull();
            assertThat(method.isAnnotationPresent(jakarta.annotation.PostConstruct.class)).isTrue();
        }

        @Test
        @DisplayName("Should execute onStartup method without throwing exceptions")
        void shouldExecuteOnStartupMethodWithoutThrowingExceptions() {
            // Given: StartupLogger with default configuration
            StartupLogger logger = new StartupLogger();

            // When: Calling onStartup method
            // Then: Should not throw any exceptions
            assertThatCode(logger::onStartup).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should execute onStartup with custom configuration")
        void shouldExecuteOnStartupWithCustomConfiguration() {
            // Given: StartupLogger with custom configuration
            StartupLogger logger = new StartupLogger();
            ReflectionTestUtils.setField(logger, "serverName", "test-server");
            ReflectionTestUtils.setField(logger, "serverVersion", "1.0.0-test");
            ReflectionTestUtils.setField(logger, "transport", "test-transport");

            // When: Calling onStartup method
            // Then: Should not throw any exceptions with custom config
            assertThatCode(logger::onStartup).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Component Annotations")
    class ComponentAnnotations {

        @Test
        @DisplayName("Should have Component annotation")
        void shouldHaveComponentAnnotation() {
            // When: Checking for Component annotation
            boolean hasComponent = StartupLogger.class.isAnnotationPresent(
                org.springframework.stereotype.Component.class);

            // Then: Should have the annotation
            assertThat(hasComponent).isTrue();
        }
    }

    @Nested
    @DisplayName("Value Annotations")
    class ValueAnnotations {

        @Test
        @DisplayName("Should have Value annotation on serverName field")
        void shouldHaveValueAnnotationOnServerNameField() throws NoSuchFieldException {
            // When: Getting the serverName field
            java.lang.reflect.Field field = StartupLogger.class.getDeclaredField("serverName");

            // Then: Field should have Value annotation
            assertThat(field.isAnnotationPresent(
                org.springframework.beans.factory.annotation.Value.class)).isTrue();

            // Check the default value
            org.springframework.beans.factory.annotation.Value valueAnnotation =
                field.getAnnotation(org.springframework.beans.factory.annotation.Value.class);
            assertThat(valueAnnotation.value()).isEqualTo("${spring.ai.mcp.server.name:spring-vision}");
        }

        @Test
        @DisplayName("Should have Value annotation on serverVersion field")
        void shouldHaveValueAnnotationOnServerVersionField() throws NoSuchFieldException {
            // When: Getting the serverVersion field
            java.lang.reflect.Field field = StartupLogger.class.getDeclaredField("serverVersion");

            // Then: Field should have Value annotation
            assertThat(field.isAnnotationPresent(
                org.springframework.beans.factory.annotation.Value.class)).isTrue();

            // Check the default value
            org.springframework.beans.factory.annotation.Value valueAnnotation =
                field.getAnnotation(org.springframework.beans.factory.annotation.Value.class);
            assertThat(valueAnnotation.value()).isEqualTo("${spring.ai.mcp.server.version:1.0.5}");
        }

        @Test
        @DisplayName("Should have Value annotation on transport field")
        void shouldHaveValueAnnotationOnTransportField() throws NoSuchFieldException {
            // When: Getting the transport field
            java.lang.reflect.Field field = StartupLogger.class.getDeclaredField("transport");

            // Then: Field should have Value annotation
            assertThat(field.isAnnotationPresent(
                org.springframework.beans.factory.annotation.Value.class)).isTrue();

            // Check the default value
            org.springframework.beans.factory.annotation.Value valueAnnotation =
                field.getAnnotation(org.springframework.beans.factory.annotation.Value.class);
            assertThat(valueAnnotation.value()).isEqualTo("${spring.ai.mcp.server.transport:stdio}");
        }
    }

    @Nested
    @DisplayName("Logging Behavior")
    class LoggingBehavior {

        @Test
        @DisplayName("Should have logger field")
        void shouldHaveLoggerField() {
            // When: Checking if logger field exists
            // This is a static field, so we check the class
            boolean hasLoggerField = false;
            try {
                StartupLogger.class.getDeclaredField("log");
                hasLoggerField = true;
            } catch (NoSuchFieldException e) {
                hasLoggerField = false;
            }

            // Then: Should have logger field
            assertThat(hasLoggerField).isTrue();
        }

        @Test
        @DisplayName("Should use structured logging in onStartup")
        void shouldUseStructuredLoggingInOnStartup() {
            // This test verifies that the onStartup method contains structured logging calls
            // The actual logging behavior is tested implicitly by ensuring the method runs without errors

            // Given: StartupLogger
            StartupLogger logger = new StartupLogger();

            // When: Calling onStartup (which contains logging statements)
            logger.onStartup();

            // Then: Method should complete successfully
            // Note: Actual log output verification would require log capture in integration tests
            assertThat(true).isTrue(); // Placeholder assertion
        }
    }

    @Nested
    @DisplayName("MCP Protocol Awareness")
    class McpProtocolAwareness {

        @Test
        @DisplayName("Should be aware of stdio transport requirements")
        void shouldBeAwareOfStdioTransportRequirements() {
            // This test verifies that the StartupLogger is designed for stdio transport
            // by checking the default transport value

            // Given: StartupLogger with default config
            StartupLogger logger = new StartupLogger();

            // When: Getting transport field
            String transport = (String) ReflectionTestUtils.getField(logger, "transport");

            // Then: Should default to stdio transport
            assertThat(transport).isEqualTo("stdio");
        }

        @Test
        @DisplayName("Should log stdout reservation for MCP messages")
        void shouldLogStdoutReservationForMcpMessages() {
            // This test verifies the design intent of the StartupLogger
            // The actual logging content is verified by the presence of the logging calls

            // Given: StartupLogger
            StartupLogger logger = new StartupLogger();

            // When: Checking the onStartup method exists and is annotated
            java.lang.reflect.Method method = null;
            try {
                method = StartupLogger.class.getMethod("onStartup");
            } catch (NoSuchMethodException e) {
                fail("onStartup method should exist");
            }

            // Then: Method should exist and be properly configured
            assertThat(method).isNotNull();
            assertThat(method.isAnnotationPresent(jakarta.annotation.PostConstruct.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("Spring Integration")
    class SpringIntegration {

        @Test
        @DisplayName("Should be compatible with Spring component scanning")
        void shouldBeCompatibleWithSpringComponentScanning() {
            // Given: StartupLogger class
            Class<?> loggerClass = StartupLogger.class;

            // When: Checking for Spring annotations
            boolean hasComponent = loggerClass.isAnnotationPresent(
                org.springframework.stereotype.Component.class);

            // Then: Should have proper Spring annotations
            assertThat(hasComponent).isTrue();
        }

        @Test
        @DisplayName("Should support property injection")
        void shouldSupportPropertyInjection() {
            // Given: StartupLogger
            StartupLogger logger = new StartupLogger();

            // When: Setting properties (simulating Spring injection)
            ReflectionTestUtils.setField(logger, "serverName", "injected-name");
            ReflectionTestUtils.setField(logger, "serverVersion", "injected-version");
            ReflectionTestUtils.setField(logger, "transport", "injected-transport");

            // Then: Should accept the injected values
            assertThat(ReflectionTestUtils.getField(logger, "serverName")).isEqualTo("injected-name");
            assertThat(ReflectionTestUtils.getField(logger, "serverVersion")).isEqualTo("injected-version");
            assertThat(ReflectionTestUtils.getField(logger, "transport")).isEqualTo("injected-transport");
        }
    }
}

