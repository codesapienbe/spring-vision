# Multi-stage build for optimized Spring Vision MCP Server

# ===== BUILD STAGE =====
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /app

# Copy pom files first for better caching
COPY pom.xml ./

# Copy all module pom.xml files for dependency resolution
COPY core/pom.xml core/
COPY starter/pom.xml starter/
COPY model/pom.xml model/
COPY mcp/pom.xml mcp/

# Download dependencies (cached if pom.xml hasn't changed)
RUN mvn dependency:go-offline -B

# Copy source code
COPY core/src core/src
COPY starter/src starter/src
COPY model/src model/src
COPY mcp/src mcp/src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests -pl mcp -am

# ===== RUNTIME STAGE =====
FROM eclipse-temurin:21-jre-jammy

# Create non-root user
RUN groupadd -r springvision && useradd -r -g springvision springvision

# Set working directory
WORKDIR /app

# Create logs directory with proper permissions
RUN mkdir -p /app/logs && chown -R springvision:springvision /app

# Copy the built JAR from builder stage
COPY --from=builder /app/mcp/target/mcp-*.jar /app/app.jar

# Change ownership to non-root user
RUN chown springvision:springvision /app/app.jar

# Switch to non-root user
USER springvision

# Expose port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
