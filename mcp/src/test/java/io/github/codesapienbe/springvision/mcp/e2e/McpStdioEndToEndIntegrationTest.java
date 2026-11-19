package io.github.codesapienbe.springvision.mcp.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The MCP server over stdio, end to end: launched as a child process with the {@code stdio}
 * profile, as a local MCP client does, speaking JSON-RPC over stdin and stdout. The vision
 * backend is a stub, so no model is loaded.
 */
class McpStdioEndToEndIntegrationTest {

    @TempDir
    static java.nio.file.Path workDir;

    private static StdioMcpProcess server;

    @BeforeAll
    static void startServer() throws Exception {
        server = StdioMcpProcess.start(StdioTestServer.class, workDir, List.of(), List.of(
                "--spring.vision.djl.online-classifier.enabled=false"));
        server.initialize();
    }

    @AfterAll
    static void stopServer() throws Exception {
        server.close();
    }

    @Test
    @DisplayName("Lists every tool, without a token")
    void listsTools() {
        assertThat(server.toolNames()).hasSize(69).contains("scan_barcode_b", "list_identities", "setup_status");
    }

    @Test
    @DisplayName("stdout carries nothing but JSON-RPC")
    void stdoutIsOnlyJsonRpc() {
        server.toolNames();
        assertThat(server.nonJsonStdout()).isEmpty();
    }

    @Test
    @DisplayName("Runs no web server")
    void noWebServer() {
        assertThat(server.stderr()).doesNotContain("Tomcat started", "Tomcat initialized");
    }

    @Test
    @DisplayName("The local caller has the shipped stdio-roles (mcp-admin): every tool runs")
    void defaultStdioRolesRunEverything() {
        assertThat(server.call("list_identities", Map.of()).denied()).isFalse();
        assertThat(server.call("recognize_license_plate_b", Map.of()).denied()).isFalse();
        assertThat(server.call("scan_barcode_b", Map.of()).denied()).isFalse();
    }

    @Test
    @DisplayName("stdio-roles decides what the local caller may run")
    void stdioRolesAreConfigurable(@TempDir java.nio.file.Path dir) throws Exception {
        try (StdioMcpProcess user = StdioMcpProcess.start(StdioTestServer.class, dir, List.of(), List.of(
                "--spring.vision.djl.online-classifier.enabled=false",
                "--spring.vision.mcp.security.stdio-roles=mcp-user"))) {
            user.initialize();

            ToolResult denied = user.call("list_identities", Map.of());
            assertThat(denied.denied()).isTrue();
            assertThat(denied.text()).contains("[mcp-user]");
            assertThat(user.call("scan_barcode_b", Map.of()).denied()).isFalse();
        }
    }

    @Test
    @DisplayName("Without stdio-roles the local caller runs nothing")
    void noStdioRolesRunNothing(@TempDir java.nio.file.Path dir) throws Exception {
        try (StdioMcpProcess none = StdioMcpProcess.start(StdioTestServer.class, dir, List.of(), List.of(
                "--spring.vision.djl.online-classifier.enabled=false",
                "--spring.vision.mcp.security.stdio-roles="))) {
            none.initialize();
            assertThat(none.call("scan_barcode_b", Map.of()).denied()).isTrue();
        }
    }

    @Test
    @DisplayName("The server exits when the client closes stdin")
    void exitsWhenStdinCloses(@TempDir java.nio.file.Path dir) throws Exception {
        try (StdioMcpProcess process = StdioMcpProcess.start(StdioTestServer.class, dir, List.of(), List.of(
                "--spring.vision.djl.online-classifier.enabled=false"))) {
            process.initialize();
            assertThat(process.closeStdinAndAwaitExit(Duration.ofSeconds(30))).isTrue();
        }
    }
}
