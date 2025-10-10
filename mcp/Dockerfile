# Use JRE instead of JDK for smaller image size
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy only the built JAR file
COPY target/spring-vision-mcp-1.0.jar app.jar

# Run as non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# MCP server runs in stdio mode - no port exposure needed
ENTRYPOINT ["java", "-jar", "app.jar"]
