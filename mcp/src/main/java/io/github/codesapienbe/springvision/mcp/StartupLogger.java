package io.github.codesapienbe.springvision.mcp;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Emits structured logs at startup to demonstrate logging configuration
 * and inform about MCP server HTTP/SSE transport.
 */
@Component
public class StartupLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    @Value("${server.port:8080}")
    private int serverPort;

    @PostConstruct
    public void onStartup() {
        log.info("=".repeat(80));
        log.info("Spring Vision MCP Server - Ready");
        log.info("=".repeat(80));
        log.info("Transport: HTTP with SSE (Server-Sent Events)");
        log.info("Server URL: http://localhost:{}", serverPort);
        log.info("MCP Endpoint: http://localhost:{}/mcp", serverPort);
        log.info("Vision capabilities: Face detection, OCR, pose estimation, and more");
        log.info("=".repeat(80));
    }
}
