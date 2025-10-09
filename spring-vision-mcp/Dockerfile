# Use OpenJDK 21 as the base image
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY target/spring-vision-mcp-1.0.jar app.jar

# Expose port 8080 for the web server
EXPOSE 8080

# Set environment variables with defaults
ENV SPRING_PROFILES_ACTIVE=default
ENV SERVER_PORT=8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
