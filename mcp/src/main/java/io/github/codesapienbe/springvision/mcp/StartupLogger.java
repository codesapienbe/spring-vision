package io.github.codesapienbe.springvision.mcp;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Emits a small set of structured logs at startup to demonstrate logging configuration.
 */
@Component
public class StartupLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    @PostConstruct
    public void onStartup() {
        log.info("MCP module startup initialized: logging is configured and operational");
    }
}
