package io.github.codesapienbe.springvision.mcp.e2e;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import io.github.codesapienbe.springvision.mcp.SpringVisionMcpServerApplication;

/**
 * Misconfiguration stops the HTTP server at start-up instead of producing a server that looks
 * healthy but rejects or allows the wrong calls.
 */
class McpStartupIntegrationTest {

    // Passed as command-line arguments: builder properties are only defaults, below application.yml.
    private static void start(String... args) {
        String[] all = java.util.stream.Stream.concat(
                java.util.stream.Stream.of("--server.port=0", "--spring.vision.djl.online-classifier.enabled=false"),
                java.util.Arrays.stream(args)).toArray(String[]::new);
        new SpringApplicationBuilder(SpringVisionMcpServerApplication.class, StubBackendConfiguration.class)
                .run(all)
                .close();
    }

    @Test
    @DisplayName("Without OAUTH2_ISSUER_URI the HTTP server does not start")
    void requiresIssuer() {
        assumeThat(System.getenv("OAUTH2_ISSUER_URI")).as("OAUTH2_ISSUER_URI set in this environment").isNull();

        assertThatThrownBy(() -> start()).hasStackTraceContaining("OAUTH2_ISSUER_URI");
    }

    @Test
    @DisplayName("A grant naming a tool that does not exist stops start-up and names it")
    void rejectsUnknownToolName() {
        assertThatThrownBy(() -> start(
                "--spring.security.oauth2.resourceserver.jwt.issuer-uri=http://127.0.0.1:9/unused",
                "--spring.vision.mcp.security.tool-access[0].role=reader",
                "--spring.vision.mcp.security.tool-access[0].tools=scan_barcode_b,count_face_b"))
                .hasStackTraceContaining("grants tools that do not exist: [count_face_b]");
    }
}
