# syntax=docker/dockerfile:1.7

# Build stage: the jar is built inside the image, so every native library it bundles (OpenCV,
# Tesseract, PyTorch, ONNX Runtime) matches the image's platform rather than the host's - a jar
# built on macOS would otherwise carry macOS natives into a Linux image.
FROM maven:3.9-eclipse-temurin-25 AS build
# Maven profiles for the build; `make bundle GPU=true` adds gpu.
ARG MAVEN_PROFILES=download-models
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests -Dgpg.skip=true -P${MAVEN_PROFILES} -pl mcp -am package \
    && cp mcp/target/mcp-[0-9]*.jar /app.jar

FROM eclipse-temurin:25-jre

# OpenCV's face detection (objdetect) loads its highgui module, which on Linux links GTK2;
# headless images do not ship it, and face detection then fails with
# "no jniopencv_highgui". On Ubuntu 24.04+ the package is libgtk2.0-0t64, which the
# libgtk2.0-0 name resolves to; older releases ship libgtk2.0-0 itself.
RUN apt-get update \
    && apt-get install -y --no-install-recommends libgtk2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app.jar app.jar

# Cap the heap as a fraction of the container's memory limit instead of the JVM's
# uncapped default, so a constrained host (e.g. a free-tier VM shared with other services)
# gets a predictable OOM-kill boundary instead of the JVM over-committing.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
