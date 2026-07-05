FROM eclipse-temurin:25-jre

ARG SPRING_VISION_VERSION

WORKDIR /app
COPY mcp/target/mcp-${SPRING_VISION_VERSION}.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
