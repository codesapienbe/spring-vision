package io.github.codesapienbe.springvision.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Spring Vision MCP Server using stdio transport.
 * This server provides computer vision operations via the Model Context Protocol (MCP)
 * using standard input/output for communication with MCP clients.
 */
@SpringBootApplication
public class SpringVisionMcpServerApplication {

    /**
     * Default constructor for {@link SpringVisionMcpServerApplication}.
     */
    public SpringVisionMcpServerApplication() {
        // Default constructor
    }

    /**
     * Main entry point for the Spring Vision MCP Server application.
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringVisionMcpServerApplication.class, args);
    }
}
