# Multi-stage build for Spring Vision MCP Server with OpenCV support

# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy parent POM and resolve dependencies
COPY pom.xml .
COPY core/pom.xml core/
COPY mcp/pom.xml mcp/
COPY persistence/pom.xml persistence/
COPY health/pom.xml health/
COPY robotics/pom.xml robotics/
COPY compreface/pom.xml compreface/
COPY deepface/pom.xml deepface/
COPY insightface/pom.xml insightface/
COPY starter/pom.xml starter/
COPY vllm/pom.xml vllm/
COPY cyber/pom.xml cyber/

# Download dependencies (cache layer)
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY core/src core/src
COPY mcp/src mcp/src
COPY persistence/src persistence/src
COPY health/src health/src

# Build the project
RUN mvn clean package -pl mcp -am -DskipTests

# Stage 2: Runtime stage with OpenCV native libraries
FROM eclipse-temurin:21-jre-jammy

# Install OpenCV and required native libraries
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    libopencv-dev \
    libopencv-core4.5d \
    libopencv-imgproc4.5d \
    libopencv-imgcodecs4.5d \
    libopencv-objdetect4.5d \
    libopencv-dnn4.5d \
    libopencv-highgui4.5d \
    libgtk2.0-0 \
    libglib2.0-0 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy the JAR from builder
COPY --from=builder /build/mcp/target/mcp-*.jar app.jar

# Copy resources (models are included in JAR, but we can override if needed)
COPY --from=builder /build/core/src/main/resources resources/

# Set environment variables
ENV JAVA_OPTS="-Xms512m -Xmx2g -Djava.library.path=/usr/lib/x86_64-linux-gnu:/usr/lib/jni"
ENV SERVER_PORT=8081
ENV SPRING_PROFILES_ACTIVE=default

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
