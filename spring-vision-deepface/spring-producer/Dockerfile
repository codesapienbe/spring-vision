# Multi-stage build: Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

# Set working directory
WORKDIR /app

# Copy the entire project structure
COPY . .

# Build the application
RUN mvn clean package -DskipTests -pl spring-vision-deepface/spring-producer

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy the JAR file from build stage
COPY --from=build /app/spring-vision-deepface/spring-producer/target/spring-vision-deepface-producer-*.jar app.jar

# Expose port
EXPOSE 8092

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8092/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
