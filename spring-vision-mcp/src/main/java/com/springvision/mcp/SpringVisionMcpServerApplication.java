package com.springvision.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Spring Vision Computer Vision Server.
 * This server provides REST API endpoints for computer vision operations,
 * serving as a foundation for MCP (Model Context Protocol) support when available.
 */
@SpringBootApplication
public class SpringVisionMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringVisionMcpServerApplication.class, args);
    }
}
