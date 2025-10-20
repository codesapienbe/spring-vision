# Runtime stage - copy pre-built artifact
FROM eclipse-temurin:21-jre-jammy

# Create non-root user
RUN groupadd -r springvision && useradd -r -g springvision springvision

# Set working directory
WORKDIR /app

# Create logs directory with proper permissions
RUN mkdir -p /app/logs && chown -R springvision:springvision /app

# Copy the pre-built JAR from local target directory
COPY target/mcp-*.jar /app/app.jar

# Change ownership to non-root user
RUN chown springvision:springvision /app/app.jar

# Switch to non-root user
USER springvision

# Expose port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
