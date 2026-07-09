FROM eclipse-temurin:25-jre

ARG SPRING_VISION_VERSION

WORKDIR /app
COPY mcp/target/mcp-${SPRING_VISION_VERSION}.jar app.jar

# Cap the heap as a fraction of the container's memory limit instead of the JVM's
# uncapped default, so a constrained host (e.g. a free-tier VM shared with Keycloak)
# gets a predictable OOM-kill boundary instead of the JVM over-committing.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
