// Add a small configuration that registers @Tool-annotated beans with Spring AI's MethodToolCallbackProvider
package io.github.codesapienbe.springvision.mcp.config;

import io.github.codesapienbe.springvision.mcp.VisionTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

@Configuration
public class ToolCallbackConfiguration {

    /**
     * Register MethodToolCallbackProvider with any @Tool-annotated beans (tool objects).
     * The MCP autoconfiguration will pick up the provided ToolCallbackProvider and
     * expose those tools to MCP clients.
     */
    @Bean
    public ToolCallbackProvider tools(@Nullable VisionTool visionTool) {
        MethodToolCallbackProvider.Builder builder = MethodToolCallbackProvider.builder();
        if (visionTool != null) {
            // register the VisionTool instance so its @Tool methods are discovered
            builder.toolObjects(visionTool);
        }
        return builder.build();
    }
}

