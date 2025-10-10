FROM openjdk:21-jdk-alpine

COPY target/basic-face-detection-1.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app.jar"]
