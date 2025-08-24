#!/bin/bash

# DETECT
SPRING_DOCKER_COMPOSE_ENABLED=false \
VISION_BACKEND=opencv \
VISION_DEEPFACE_ENABLED=false \
VISION_COMPREFACE_ENABLED=false \
SERVER_PORT=9999 \
mvn -pl spring-vision-examples/picocli-application spring-boot:run \
  -Dspring-boot.run.arguments="--detect ~/mockdata/spring-vision/selfie-a.jpg --format json"


SPRING_DOCKER_COMPOSE_ENABLED=false \
VISION_BACKEND=opencv \
VISION_DEEPFACE_ENABLED=false \
VISION_COMPREFACE_ENABLED=false \
SERVER_PORT=9999 \
mvn -pl spring-vision-examples/picocli-application spring-boot:run \
  -Dspring-boot.run.arguments="--detect ~/mockdata/spring-vision/selfie-b.jpg --format json"


# VERIFY
SPRING_DOCKER_COMPOSE_ENABLED=false \
VISION_BACKEND=opencv \
VISION_DEEPFACE_ENABLED=false \
VISION_COMPREFACE_ENABLED=false \
SERVER_PORT=9999 \
mvn -pl spring-vision-examples/picocli-application spring-boot:run \
  -Dspring-boot.run.arguments="--verify ~/mockdata/spring-vision/selfie-a.jpg ~/mockdata/spring-vision/selfie-b.jpg --metric cosine --format json"


# VERIFY BATCH
SPRING_DOCKER_COMPOSE_ENABLED=false \
VISION_BACKEND=opencv \
VISION_DEEPFACE_ENABLED=false \
VISION_COMPREFACE_ENABLED=false \
SERVER_PORT=9999 \
mvn -pl spring-vision-examples/picocli-application spring-boot:run \
  -Dspring-boot.run.arguments="--verify-batch ~/mockdata/spring-vision/selfie-a.jpg ~/mockdata/spring-vision/batch --metric cosine --threshold 0.35 --format csv --progress"


# EMBED
SPRING_DOCKER_COMPOSE_ENABLED=false \
VISION_BACKEND=opencv \
VISION_DEEPFACE_ENABLED=false \
VISION_COMPREFACE_ENABLED=false \
SERVER_PORT=9999 \
mvn -pl spring-vision-examples/picocli-application spring-boot:run \
  -Dspring-boot.run.arguments="--embed ~/mockdata/spring-vision/selfie-a.jpg --format json --truncate 8"

