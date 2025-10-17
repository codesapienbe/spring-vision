# Base image for Spring Vision - DJL Serving with JDK 21 installed
FROM deepjavalibrary/djl-serving:0.35.0-cpu-full

# Install JDK 21 as root
USER root
RUN apt-get update -y && \
    apt-get install -y openjdk-21-jdk && \
    rm -rf /var/lib/apt/lists/*

# Set the default java to the newly installed JDK
RUN update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-21-openjdk-amd64/bin/java 1 && \
    update-alternatives --set java /usr/lib/jvm/java-21-openjdk-amd64/bin/java

# JAVA_HOME for JDK21
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# DJL cache directory environment (created but left empty in this base image)
ENV AI_DJL_CACHE=/opt/djl-cache
ENV JAVA_TOOL_OPTIONS="-Dai.djl.repository.cache.dir=${AI_DJL_CACHE}"

# Create the cache directory and ensure correct ownership for the runtime user
RUN mkdir -p ${AI_DJL_CACHE} && chown -R djl:djl ${AI_DJL_CACHE} || true

# Switch back to the non-root user used by djl-serving
USER djl

# NOTE: Intentionally no COPY of application files and no ENTRYPOINT so this can be used
# as a reusable base image named `spring-vision-base`.

