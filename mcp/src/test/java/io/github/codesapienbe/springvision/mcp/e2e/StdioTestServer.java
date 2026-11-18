package io.github.codesapienbe.springvision.mcp.e2e;

import org.springframework.boot.SpringApplication;

import io.github.codesapienbe.springvision.mcp.SpringVisionMcpServerApplication;

/**
 * Starts the real MCP server application with the {@link StubBackendConfiguration}, for the
 * stdio end-to-end tests, which run it as a separate process.
 */
public final class StdioTestServer {

    private StdioTestServer() {
    }

    /**
     * @param args Spring Boot arguments, e.g. {@code --spring.profiles.active=stdio}.
     */
    public static void main(String[] args) {
        SpringApplication.run(new Class<?>[] {SpringVisionMcpServerApplication.class, StubBackendConfiguration.class},
                args);
    }
}
