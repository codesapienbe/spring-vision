# Multi-stage build to create a custom JRE with necessary modules
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY target/mcp-1.0.jar app.jar

# Create a custom JRE with minimal modules required for Spring Boot and vision processing
RUN jlink \
    --add-modules java.base,java.logging,java.xml,java.sql,java.naming,java.desktop,java.management,java.net.http,java.security.jgss,jdk.unsupported \
    --output /customjre \
    --strip-debug \
    --compress 2 \
    --no-header-files \
    --no-man-pages

# Final stage: Use Alpine Linux for a small base image
FROM alpine:latest

# Install CA certificates for HTTPS support
RUN apk add --no-cache ca-certificates

# Copy the custom JRE from the builder stage
COPY --from=builder /customjre /jre

# Copy the application JAR
COPY --from=builder /app/app.jar /app.jar

# Run as non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Set JAVA_HOME to the custom JRE
ENV JAVA_HOME=/jre

# MCP server runs in stdio mode - no port exposure needed
ENTRYPOINT ["/jre/bin/java", "-jar", "/app.jar"]
