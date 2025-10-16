# Multi-stage Dockerfile: native-builder stage prepares native libs, final stage is minimal runtime
# Build from project root: docker build -t spring-vision:latest -f mcp/Dockerfile .

# --- Native builder stage ---
FROM ubuntu:22.04 AS native-builder

# Install only what we need to fetch native libs and strip unnecessary files
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    ca-certificates wget libglib2.0-0 && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /native-libs

# Download or install the specific native .so files we need (OpenCV libs). Using apt-get in this stage
# keeps build-time tooling out of the final image. We install minimal packages and then copy only the
# needed .so files into /native-libs for the runtime image.
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    libopencv-core4.5d libopencv-imgproc4.5d libopencv-imgcodecs4.5d libopencv-objdetect4.5d libopencv-dnn4.5d \
    libopencv-highgui4.5d libopencv-videoio4.5d \
    libglib2.0-0 && \
    rm -rf /var/lib/apt/lists/* && \
    mkdir -p /native-libs/usr/lib && \
    find /usr/lib/x86_64-linux-gnu -name 'libopencv*' -exec cp -L {} /native-libs/usr/lib/ \; && \
    find /usr/lib/x86_64-linux-gnu -name 'libglib*' -exec cp -L {} /native-libs/usr/lib/ \; && \
    ls -la /native-libs/usr/lib/

# Strip debug symbols to reduce size (if present)
RUN apt-get update && apt-get install -y --no-install-recommends binutils && \
    find /native-libs -type f -name '*.so*' -exec strip --strip-unneeded {} + 2>/dev/null || true && \
    rm -rf /var/lib/apt/lists/*


# --- Final runtime stage ---
FROM eclipse-temurin:21-jre-jammy

# Create app directory
WORKDIR /app

# Copy only the built JAR into the final image
COPY mcp/target/mcp-*.jar app.jar

# Copy required native libraries from the native-builder stage
COPY --from=native-builder /native-libs/usr/lib /usr/lib/x86_64-linux-gnu/

# Install minimal runtime dependencies for OpenCV and JavaCV
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    libgomp1 libglib2.0-0 libsm6 libxext6 libxrender1 libfontconfig1 \
    libgtk2.0-0 libavcodec58 libavformat58 libswscale5 && \
    rm -rf /var/lib/apt/lists/*

# Set environment variables for stdio transport
# Note: JavaCV extracts its native libraries to a temp directory at runtime, so we don't need to specify java.library.path for JavaCV
ENV JAVA_OPTS="--enable-native-access=ALL-UNNAMED -Xms512m -Xmx2g"
ENV SPRING_PROFILES_ACTIVE=default

# Create logs directory
RUN mkdir -p logs

# Run the application in stdio mode (no port exposure needed)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
