package io.github.codesapienbe.springvision.mcp;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import net.logstash.logback.argument.StructuredArguments;

/**
 * Emits structured JSON logs at startup for the MCP server using Streamable-HTTP transport.
 * All logs are JSON formatted and sent to stderr/file, consistent with the rest of the app.
 */
@Component
public class StartupLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    @Value("${spring.ai.mcp.server.name:spring-vision}")
    private String serverName = "spring-vision";

    @Value("${spring.ai.mcp.server.version:1.0.5}")
    private String serverVersion = "1.0.5";

    @Value("${spring.ai.mcp.server.protocol:STREAMABLE}")
    private String protocol = "STREAMABLE";

    @Value("${server.port:8080}")
    private String serverPort = "8080";

    @PostConstruct
    public void onStartup() {
        // Log startup with structured JSON format
        log.info("Spring Vision MCP Server starting",
            StructuredArguments.keyValue("event", "mcp_server_startup"),
            StructuredArguments.keyValue("server_name", serverName),
            StructuredArguments.keyValue("server_version", serverVersion),
            StructuredArguments.keyValue("protocol", protocol),
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

        // Log Streamable-HTTP transport configuration
        log.info("MCP Streamable-HTTP transport configured",
            StructuredArguments.keyValue("event", "transport_config"),
            StructuredArguments.keyValue("endpoint", "/mcp"),
            StructuredArguments.keyValue("port", serverPort),
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
