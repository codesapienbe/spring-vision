# 1. Start with the djl-serving base image
FROM codesapienbe/spring-vision-base:latest

# Ensure application and per-user cache directories exist
RUN mkdir -p /app /app/logs /home/djl/.springvision/djl || true
COPY target/mcp-*.jar /app/app.jar
RUN chown -R djl:djl /app /home/djl/.springvision || true

# Switch to the non-root user that djl-serving uses
USER djl

# Start the application directly; DJL cache path is set from application.yml
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
