# 1. Start with the djl-serving base image
FROM codesapienbe/spring-vision-base:latest

# Switch to root to create directories and set permissions
USER root

# Ensure application and per-user cache directories exist
RUN mkdir -p /app /app/logs /home/djl/.springvision/djl || true
COPY target/mcp-*.jar /app/app.jar
# Set proper ownership
RUN chown -R djl:djl /app /home/djl/.springvision || true

# Switch to the non-root user that djl-serving uses
USER djl

# Start the application directly; DJL cache path is set from application.yml
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
