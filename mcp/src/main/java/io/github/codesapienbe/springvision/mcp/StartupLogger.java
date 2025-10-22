package io.github.codesapienbe.springvision.mcp;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import net.logstash.logback.argument.StructuredArguments;

/**
 * Emits structured JSON logs at startup for the MCP server using stdio transport.
 * All logs are JSON formatted and sent to stderr to avoid interfering with MCP protocol on stdout.
 */
@Component
public class StartupLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    @Value("${spring.ai.mcp.server.name:spring-vision}")
    private String serverName = "spring-vision";

    @Value("${spring.ai.mcp.server.version:1.0.5}")
    private String serverVersion = "1.0.5";

    @Value("${spring.ai.mcp.server.transport:stdio}")
    private String transport = "stdio";

    @PostConstruct
    public void onStartup() {
        // Log startup with structured JSON format
        log.info("Spring Vision MCP Server starting", 
            StructuredArguments.keyValue("event", "mcp_server_startup"),
            StructuredArguments.keyValue("server_name", serverName),
            StructuredArguments.keyValue("server_version", serverVersion),
            StructuredArguments.keyValue("transport", transport),
            StructuredArguments.keyValue("protocol", "stdio"),
            StructuredArguments.keyValue("stdout_reserved_for", "MCP JSON-RPC messages"),
            StructuredArguments.keyValue("logs_output", "stderr and file (JSON format)")
        );

        // Log capabilities
        log.info("MCP Server capabilities loaded",
            StructuredArguments.keyValue("event", "mcp_capabilities"),
            StructuredArguments.keyValue("capabilities", Map.of(
                "face_detection", true,
                "ocr", true,
                "pose_estimation", true,
                "object_detection", true,
                "image_classification", true
            ))
        );

        // Log stdio transport configuration
        log.info("MCP stdio transport configured",
            StructuredArguments.keyValue("event", "transport_config"),
            StructuredArguments.keyValue("stdin", "MCP JSON-RPC requests"),
            StructuredArguments.keyValue("stdout", "MCP JSON-RPC responses"),
            StructuredArguments.keyValue("stderr", "Application logs (JSON)"),
            StructuredArguments.keyValue("log_files", new String[]{
                "logs/mcp.json.log",
                "logs/mcp-error.json.log"
            })
        );

        log.info("Spring Vision MCP Server ready",
            StructuredArguments.keyValue("event", "mcp_server_ready"),
            StructuredArguments.keyValue("status", "ready"),
            StructuredArguments.keyValue("awaiting", "MCP initialize message")
        );
    }
}
