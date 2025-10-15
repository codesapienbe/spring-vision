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
    libopencv-core4.5 libopencv-imgproc4.5 libopencv-imgcodecs4.5 libopencv-objdetect4.5 libopencv-dnn4.5 libglib2.0-0 && \
    rm -rf /var/lib/apt/lists/* && \
    mkdir -p /native-libs/usr/lib && \
    cp -L /usr/lib/x86_64-linux-gnu/libopencv* /native-libs/usr/lib/ 2>/dev/null || true && \
    cp -L /usr/lib/x86_64-linux-gnu/libglib* /native-libs/usr/lib/ 2>/dev/null || true

# Strip debug symbols to reduce size (if present)
RUN apt-get update && apt-get install -y --no-install-recommends binutils && \
    find /native-libs -type f -name '*.so*' -exec strip --strip-unneeded {} + || true && \
    rm -rf /var/lib/apt/lists/*


# --- Final runtime stage ---
FROM eclipse-temurin:21-jre-jammy

# Create app directory
WORKDIR /app

# Copy only the built JAR into the final image
COPY mcp/target/mcp-*.jar app.jar

# Copy required native libraries from the native-builder stage
COPY --from=native-builder /native-libs/usr/lib /usr/lib/x86_64-linux-gnu

# Set environment variables for stdio transport
ENV JAVA_OPTS="--enable-native-access=ALL-UNNAMED -Xms512m -Xmx2g -Djava.library.path=/usr/lib/x86_64-linux-gnu:/usr/lib/jni"
ENV SPRING_PROFILES_ACTIVE=default

# Create logs directory
RUN mkdir -p logs

# Run the application in stdio mode (no port exposure needed)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
