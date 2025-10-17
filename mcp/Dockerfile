# 1. Start with the djl-serving base image
FROM codesapienbe/spring-vision-base:latest

# 2. Prepare application directory and copy artifacts as root
RUN mkdir -p /app || true
COPY target/mcp-*.jar /app/app.jar
COPY target/djl-cache ${AI_DJL_CACHE}
RUN chown -R djl:djl /app ${AI_DJL_CACHE} || true

# 3. Switch back to the non-root user that djl-serving uses
USER djl

# 4. Define the entrypoint to run your app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
