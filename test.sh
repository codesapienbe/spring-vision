#!/bin/bash

# Build all modules once to ensure latest APIs are installed locally
SPRING_DOCKER_COMPOSE_ENABLED=false \
SERVER_PORT=9999 \
mvn -DskipTests install || exit 1

if [ $? -ne 0 ]; then
  echo "BUILD failed"
  exit 1
fi

# DETECT
SPRING_DOCKER_COMPOSE_ENABLED=false \
VISION_BACKEND=opencv \
VISION_DEEPFACE_ENABLED=false \
VISION_COMPREFACE_ENABLED=false \
SERVER_PORT=9999 \
mvn -f spring-vision-examples/picocli-application/pom.xml -am org.springframework.boot:spring-boot-maven-plugin:3.2.8:run \
  -Dspring-boot.run.arguments="--detect ~/mockdata/spring-vision/selfie-a.jpg --format json"

if [ $? -ne 0 ]; then
  echo "DETECT failed"
  exit 1
fi



SPRING_DOCKER_COMPOSE_ENABLED=false \
VISION_BACKEND=opencv \
VISION_DEEPFACE_ENABLED=false \
VISION_COMPREFACE_ENABLED=false \
SERVER_PORT=9999 \
mvn -f spring-vision-examples/picocli-application/pom.xml -am org.springframework.boot:spring-boot-maven-plugin:3.2.8:run \
  -Dspring-boot.run.arguments="--detect ~/mockdata/spring-vision/selfie-b.jpg --format json"

if [ $? -ne 0 ]; then
  echo "DETECT failed"
  exit 1
fi


# VERIFY
SPRING_DOCKER_COMPOSE_ENABLED=false \
VISION_BACKEND=opencv \
VISION_DEEPFACE_ENABLED=false \
VISION_COMPREFACE_ENABLED=false \
SERVER_PORT=9999 \
mvn -f spring-vision-examples/picocli-application/pom.xml -am org.springframework.boot:spring-boot-maven-plugin:3.2.8:run \
  -Dspring-boot.run.arguments="--verify ~/mockdata/spring-vision/selfie-a.jpg ~/mockdata/spring-vision/selfie-b.jpg --metric cosine --format json"


if [ $? -ne 0 ]; then
  echo "VERIFY failed"
  exit 1
fi

# VERIFY BATCH
SPRING_DOCKER_COMPOSE_ENABLED=false \
VISION_BACKEND=opencv \
VISION_DEEPFACE_ENABLED=false \
VISION_COMPREFACE_ENABLED=false \
SERVER_PORT=9999 \
mvn -f spring-vision-examples/picocli-application/pom.xml -am org.springframework.boot:spring-boot-maven-plugin:3.2.8:run \
  -Dspring-boot.run.arguments="--verify-batch ~/mockdata/spring-vision/selfie-a.jpg ~/mockdata/spring-vision/batch --metric cosine --threshold 0.35 --format csv --progress"

if [ $? -ne 0 ]; then
  echo "VERIFY BATCH failed"
  exit 1
fi

# EMBED
SPRING_DOCKER_COMPOSE_ENABLED=false \
VISION_BACKEND=opencv \
VISION_DEEPFACE_ENABLED=false \
VISION_COMPREFACE_ENABLED=false \
SERVER_PORT=9999 \
mvn -f spring-vision-examples/picocli-application/pom.xml -am org.springframework.boot:spring-boot-maven-plugin:3.2.8:run \
  -Dspring-boot.run.arguments="--embed ~/mockdata/spring-vision/selfie-a.jpg --format json --truncate 8"

if [ $? -ne 0 ]; then
  echo "EMBED failed"
  exit 1
fi


# OBSCURE
SPRING_DOCKER_COMPOSE_ENABLED=false \
VISION_BACKEND=opencv \
VISION_DEEPFACE_ENABLED=false \
VISION_COMPREFACE_ENABLED=false \
SERVER_PORT=9999 \
mvn -f spring-vision-examples/picocli-application/pom.xml -am org.springframework.boot:spring-boot-maven-plugin:3.2.8:run \
  -Dspring-boot.run.arguments="--obscure ~/mockdata/spring-vision/selfie-a.jpg ~/mockdata/spring-vision/selfie-a-obscured.jpg"

if [ $? -ne 0 ]; then
  echo "OBSCURE failed"
  exit 1
fi



