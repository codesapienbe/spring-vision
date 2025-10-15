# Spring Vision MCP Server - Runtime container
# Assumes the JAR is already built locally with: mvn clean package -pl mcp -am
# Build from project root: docker build -t spring-vision:latest -f mcp/Dockerfile .

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
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy the pre-built JAR from local target directory (build context is project root)
COPY mcp/target/mcp-*.jar app.jar

# Copy resources (models are included in JAR, but we can override if needed)
COPY core/src/main/resources resources/

# Set environment variables for stdio transport
ENV JAVA_OPTS="-Xms512m -Xmx2g -Djava.library.path=/usr/lib/x86_64-linux-gnu:/usr/lib/jni"
ENV SPRING_PROFILES_ACTIVE=default

# Create logs directory
RUN mkdir -p logs

# Run the application in stdio mode (no port exposure needed)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
