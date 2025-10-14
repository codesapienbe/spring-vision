# DevOps Guide: Optimized Container for MCP Server

## Overview

The MCP (Model Context Protocol) server for Spring Vision has been optimized for minimal image size and efficient deployment. This document explains the containerization strategy and optimizations applied to the Dockerfile.

## Container Optimization Strategy

### Multi-Stage Build

The Dockerfile uses a multi-stage build approach to separate the build environment from the runtime environment:

1. **Builder Stage**: Uses Eclipse Temurin JDK 21 on Alpine Linux to create a custom JRE
2. **Runtime Stage**: Uses Alpine Linux with the custom JRE and application JAR

### Custom JRE Creation

Instead of using the full Java Runtime Environment (JRE), we create a minimal JRE using `jlink` that includes only the necessary Java modules:

- **Included Modules**:
    - `java.base`: Core Java classes
    - `java.logging`: Logging framework
    - `java.xml`: XML processing
    - `java.sql`: Database connectivity
    - `java.naming`: Naming and directory services
    - `java.desktop`: GUI and image processing (required for vision capabilities)
    - `java.management`: JMX management
    - `java.net.http`: HTTP client
    - `java.security.jgss`: Kerberos authentication
    - `jdk.unsupported`: Internal APIs used by Spring Boot

- **Optimizations Applied**:
    - `--strip-debug`: Removes debug information
    - `--compress 2`: Applies ZIP compression
    - `--no-header-files`: Excludes header files
    - `--no-man-pages`: Excludes manual pages

### Base Image Selection

- **Runtime**: Alpine Linux (latest) - minimal Linux distribution (~5MB)
- **Security**: CA certificates included for HTTPS support
- **User**: Non-root user (`spring:spring`) for security best practices

## Benefits

- **Reduced Image Size**: Custom JRE is significantly smaller than full JRE
- **Faster Deployment**: Smaller images transfer and start faster
- **Security**: Minimal attack surface with only required components
- **Performance**: Optimized JRE loads faster and uses less memory

## Build Process

1. Build the application JAR using Maven
2. Run `docker build` in the `mcp/` directory
3. The multi-stage build automatically creates the optimized image

## Usage

The container runs the MCP server in stdio mode, suitable for integration with MCP clients. No ports are exposed as communication happens through standard input/output.

## Monitoring and Maintenance

- Monitor container logs for any missing module errors
- If additional Java modules are needed, add them to the `--add-modules` list in the Dockerfile
- Regularly update the base images for security patches
