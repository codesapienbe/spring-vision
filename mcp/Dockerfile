# 1. Start with the djl-serving base image
FROM deepjavalibrary/djl-serving:0.35.0-cpu-full

# 2. Switch to the root user to install software
USER root

# 3. Install JDK 21 using apt-get
#    The `&& rm -rf /var/lib/apt/lists/*` cleans up the apt cache to keep the layer small.
RUN apt-get update -y && \
    apt-get install -y openjdk-21-jdk && \
    rm -rf /var/lib/apt/lists/*

# 4. Set the default Java version to JDK 21
#    This command updates the system to point 'java' to your new installation.
RUN update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-21-openjdk-amd64/bin/java 1 && \
    update-alternatives --set java /usr/lib/jvm/java-21-openjdk-amd64/bin/java

# 5. Set the JAVA_HOME for the new JDK
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Set DJL cache location inside the image. We'll copy the prefilled cache into this path.
ENV AI_DJL_CACHE=/opt/djl-cache
ENV JAVA_TOOL_OPTIONS="-Dai.djl.repository.cache.dir=${AI_DJL_CACHE}"

# 6. Prepare application directory and copy artifacts as root
RUN mkdir -p /app || true
# Copy the repackaged Spring Boot jar from the module's target directory
COPY target/mcp-*.jar /app/app.jar

# Copy the prefilled DJL cache (populated during the package phase) into the image.
# The build context is the mcp module directory, so target/djl-cache is available.
COPY target/djl-cache ${AI_DJL_CACHE}

# Ensure the non-root runtime user owns the application and cache directories
RUN chown -R djl:djl /app ${AI_DJL_CACHE} || true

# 7. Switch back to the non-root user that djl-serving uses
USER djl

# 8. Define the entrypoint to run your app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
