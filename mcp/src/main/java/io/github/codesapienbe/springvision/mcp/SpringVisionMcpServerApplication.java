package io.github.codesapienbe.springvision.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

import org.springframework.core.env.Environment;

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
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SpringVisionMcpServerApplication.class);
        // Ensure the DJL repository cache dir system property is set early from application.yml
        app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {
            Environment env = event.getEnvironment();
            String modelCache = env.getProperty("spring.vision.djl.model-cache-dir");
            if (modelCache != null && !modelCache.isBlank()) {
                System.setProperty("ai.djl.repository.cache.dir", modelCache);
                System.out.println("[startup] Set ai.djl.repository.cache.dir=" + modelCache);
            }
        });
        app.run(args);
    }
}
