package io.github.codesapienbe.springvision.mcp.config;

import io.github.codesapienbe.springvision.mcp.VisionTool;
import io.github.codesapienbe.springvision.core.VectorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

/**
 * Configuration for registering Spring AI Tool Callbacks.
 * This class registers any @Tool-annotated beans with Spring AI's MethodToolCallbackProvider.
 */
@Configuration
public class ToolCallbackConfiguration {

    /**
     * Default constructor for {@link ToolCallbackConfiguration}.
     */
    public ToolCallbackConfiguration() {
        // Default constructor
    }

    /**
     * Register MethodToolCallbackProvider with any @Tool-annotated beans (tool objects).
     * The MCP autoconfiguration will pick up the provided ToolCallbackProvider and
     * expose those tools to MCP clients.
     * @param visionTool The VisionTool bean, which may be null.
     * @return A ToolCallbackProvider that provides the VisionTool.
     */
    @Bean
    public ToolCallbackProvider tools(@Nullable VisionTool visionTool) {
        MethodToolCallbackProvider.Builder builder = MethodToolCallbackProvider.builder();
        if (visionTool != null) {
            // register the VisionTool instance so its @Tool methods are discovered
            builder.toolObjects(visionTool);
        } else {
            // When no VisionTool is available, create an empty provider
            builder.toolObjects(new Object[0]);
        }
        return builder.build();
    }

    @Bean
    public VectorService inMemoryVectorService() {
        return new io.github.codesapienbe.springvision.mcp.config.InMemoryVectorService();
    }
}
